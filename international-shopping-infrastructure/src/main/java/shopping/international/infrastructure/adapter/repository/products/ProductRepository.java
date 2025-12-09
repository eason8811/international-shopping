package shopping.international.infrastructure.adapter.repository.products;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.repository.products.IProductRepository;
import shopping.international.domain.model.aggregate.products.Product;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.entity.products.ProductSpecValue;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.enums.products.SpecType;
import shopping.international.domain.model.vo.products.ProductI18n;
import shopping.international.domain.model.vo.products.ProductImage;
import shopping.international.domain.model.vo.products.ProductSpecI18n;
import shopping.international.domain.model.vo.products.ProductSpecValueI18n;
import shopping.international.infrastructure.dao.products.ProductCategoryMapper;
import shopping.international.infrastructure.dao.products.ProductI18nMapper;
import shopping.international.infrastructure.dao.products.ProductImageMapper;
import shopping.international.infrastructure.dao.products.ProductMapper;
import shopping.international.infrastructure.dao.products.po.*;
import shopping.international.types.exceptions.ConflictException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static shopping.international.types.utils.FieldValidateUtils.normalizeLocale;
import static shopping.international.types.utils.FieldValidateUtils.normalizeTags;

/**
 * 基于 MyBatis-Plus 的商品聚合仓储实现
 *
 * <p>负责组合查询商品基础信息, 图库, 规格/规格值及多语言数据, 并提供默认 SKU 与库存的持久化操作</p>
 */
@Repository
@RequiredArgsConstructor
public class ProductRepository implements IProductRepository {

    /**
     * 商品主表 Mapper
     */
    private final ProductMapper productMapper;
    /**
     * 分类 Mapper
     */
    private final ProductCategoryMapper productCategoryMapper;
    /**
     * 商品图片 Mapper
     */
    private final ProductImageMapper productImageMapper;
    /**
     * 商品多语言 Mapper
     */
    private final ProductI18nMapper productI18nMapper;
    /**
     * Jackson 对象映射器, 用于解析 JSON 字段
     */
    private final ObjectMapper OBJECT_MAPPER;

    /**
     * 按 ID 查询商品聚合
     *
     * @param productId 商品 ID
     * @return 商品聚合, 不存在返回空
     */
    @Override
    public @NotNull Optional<Product> findById(@NotNull Long productId) {
        ProductPO po = productMapper.selectAggregateById(productId);
        if (po == null || po.getId() == null)
            return Optional.empty();
        return Optional.of(buildAggregate(po));
    }

    /**
     * 按 slug 查询上架商品
     *
     * @param slug   商品 slug 或本地化 slug
     * @param locale 请求语言, 可用于匹配 i18n slug
     * @return 上架商品聚合, 不存在或未上架返回空
     */
    @Override
    public @NotNull Optional<Product> findOnSaleBySlug(@NotNull String slug, @NotNull String locale) {
        ProductPO po = productMapper.selectOnSaleAggregateBySlug(slug, locale);
        if (po == null || po.getId() == null)
            return Optional.empty();
        return Optional.of(buildAggregate(po));
    }

    /**
     * 查询分类 slug
     *
     * @param categoryId 分类 ID
     * @return 分类 slug, 不存在返回 null
     */
    @Override
    public @Nullable String findCategorySlug(@NotNull Long categoryId) {
        ProductCategoryPO po = productCategoryMapper.selectById(categoryId);
        return po == null ? null : po.getSlug();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDefaultSkuId(@NotNull Long productId, @Nullable Long defaultSkuId) {
        LambdaUpdateWrapper<ProductPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ProductPO::getId, productId)
                .set(ProductPO::getDefaultSkuId, defaultSkuId)
                .set(ProductPO::getUpdatedAt, LocalDateTime.now());
        productMapper.update(null, wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockTotal(@NotNull Long productId, int stock) {
        LambdaUpdateWrapper<ProductPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ProductPO::getId, productId)
                .set(ProductPO::getStockTotal, stock)
                .set(ProductPO::getUpdatedAt, LocalDateTime.now());
        productMapper.update(null, wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull Product save(@NotNull Product product) {
        ProductPO po = toProductPO(product);
        try {
            productMapper.insert(po);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("商品唯一约束冲突", e);
        }
        product.assignId(po.getId());
        persistGallery(po.getId(), product.getGallery());
        return findById(po.getId()).orElseThrow(() -> new ConflictException("商品保存后回读失败"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull Product updateBasic(@NotNull Product product, boolean replaceGallery) {
        LambdaUpdateWrapper<ProductPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ProductPO::getId, product.getId())
                .set(ProductPO::getSlug, product.getSlug())
                .set(ProductPO::getTitle, product.getTitle())
                .set(ProductPO::getSubtitle, product.getSubtitle())
                .set(ProductPO::getDescription, product.getDescription())
                .set(ProductPO::getCategoryId, product.getCategoryId())
                .set(ProductPO::getBrand, product.getBrand())
                .set(ProductPO::getCoverImageUrl, product.getCoverImageUrl())
                .set(ProductPO::getSkuType, product.getSkuType().name())
                .set(ProductPO::getStatus, product.getStatus().name())
                .set(ProductPO::getTags, writeTags(product.getTags()))
                .set(ProductPO::getUpdatedAt, LocalDateTime.now());
        try {
            productMapper.update(null, wrapper);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("商品唯一约束冲突", e);
        }
        if (replaceGallery) {
            productImageMapper.delete(new LambdaQueryWrapper<ProductImagePO>().eq(ProductImagePO::getProductId, product.getId()));
            persistGallery(product.getId(), product.getGallery());
        }
        return findById(product.getId()).orElseThrow(() -> new ConflictException("商品更新后回读失败"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceGallery(@NotNull Long productId, @NotNull List<ProductImage> gallery) {
        productImageMapper.delete(new LambdaQueryWrapper<ProductImagePO>().eq(ProductImagePO::getProductId, productId));
        persistGallery(productId, gallery);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveI18n(@NotNull Long productId, @NotNull ProductI18n i18n) {
        ProductI18nPO po = ProductI18nPO.builder()
                .productId(productId)
                .locale(i18n.getLocale())
                .title(i18n.getTitle())
                .subtitle(i18n.getSubtitle())
                .description(i18n.getDescription())
                .slug(i18n.getSlug())
                .tags(writeTags(i18n.getTags()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        try {
            productI18nMapper.insert(po);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("商品多语言唯一约束冲突", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateI18n(@NotNull Long productId, @NotNull ProductI18n i18n) {
        LambdaUpdateWrapper<ProductI18nPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ProductI18nPO::getProductId, productId)
                .eq(ProductI18nPO::getLocale, i18n.getLocale())
                .set(ProductI18nPO::getTitle, i18n.getTitle())
                .set(ProductI18nPO::getSubtitle, i18n.getSubtitle())
                .set(ProductI18nPO::getDescription, i18n.getDescription())
                .set(ProductI18nPO::getSlug, i18n.getSlug())
                .set(ProductI18nPO::getTags, writeTags(i18n.getTags()))
                .set(ProductI18nPO::getUpdatedAt, LocalDateTime.now());
        productI18nMapper.update(null, wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull List<Product> list(@Nullable ProductStatus status, @Nullable SkuType skuType,
                                       @Nullable Long categoryId, @Nullable String keyword, @Nullable String tag,
                                       boolean includeDeleted, int offset, int limit) {
        LambdaQueryWrapper<ProductPO> wrapper = buildListWrapper(status, skuType, categoryId, keyword, tag, includeDeleted);
        wrapper.last("LIMIT " + offset + "," + limit);
        List<ProductPO> pos = productMapper.selectList(wrapper);
        if (pos == null || pos.isEmpty())
            return Collections.emptyList();
        Map<Long, List<ProductImagePO>> galleryMap = loadGallery(pos.stream().map(ProductPO::getId).toList());
        List<Product> result = new ArrayList<>();
        for (ProductPO po : pos) {
            List<ProductImagePO> imagePOs = galleryMap.getOrDefault(po.getId(), Collections.emptyList());
            List<ProductImage> gallery = toGallery(imagePOs);
            List<String> tags = parseTags(po.getTags());
            result.add(Product.reconstitute(
                    po.getId(), po.getSlug(), po.getTitle(), po.getSubtitle(), po.getDescription(),
                    po.getCategoryId(), po.getBrand(), po.getCoverImageUrl(),
                    po.getStockTotal() == null ? 0 : po.getStockTotal(),
                    po.getSaleCount() == null ? 0 : po.getSaleCount(),
                    SkuType.from(po.getSkuType()),
                    ProductStatus.from(po.getStatus()),
                    po.getDefaultSkuId(), tags, gallery, Collections.emptyList(), Collections.emptyList(),
                    po.getCreatedAt(), po.getUpdatedAt()
            ));
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long count(@Nullable ProductStatus status, @Nullable SkuType skuType,
                      @Nullable Long categoryId, @Nullable String keyword, @Nullable String tag, boolean includeDeleted) {
        LambdaQueryWrapper<ProductPO> wrapper = buildListWrapper(status, skuType, categoryId, keyword, tag, includeDeleted);
        Long total = productMapper.selectCount(wrapper);
        return total == null ? 0 : total;
    }

    /**
     * 将领域聚合转换为持久化对象
     *
     * @param product 商品聚合
     * @return 持久化对象
     */
    private ProductPO toProductPO(@NotNull Product product) {
        return ProductPO.builder()
                .id(product.getId())
                .slug(product.getSlug())
                .title(product.getTitle())
                .subtitle(product.getSubtitle())
                .description(product.getDescription())
                .categoryId(product.getCategoryId())
                .brand(product.getBrand())
                .coverImageUrl(product.getCoverImageUrl())
                .stockTotal(product.getStockTotal())
                .saleCount(product.getSaleCount())
                .skuType(product.getSkuType().name())
                .status(product.getStatus().name())
                .defaultSkuId(product.getDefaultSkuId())
                .tags(writeTags(product.getTags()))
                .createdAt(product.getCreatedAt() == null ? LocalDateTime.now() : product.getCreatedAt())
                .updatedAt(product.getUpdatedAt() == null ? LocalDateTime.now() : product.getUpdatedAt())
                .build();
    }

    /**
     * 构建分页查询条件
     */
    private LambdaQueryWrapper<ProductPO> buildListWrapper(@Nullable ProductStatus status, @Nullable SkuType skuType,
                                                           @Nullable Long categoryId, @Nullable String keyword,
                                                           @Nullable String tag, boolean includeDeleted) {
        LambdaQueryWrapper<ProductPO> wrapper = new LambdaQueryWrapper<>();
        if (status != null)
            wrapper.eq(ProductPO::getStatus, status.name());
        else if (!includeDeleted)
            wrapper.ne(ProductPO::getStatus, ProductStatus.DELETED.name());
        if (skuType != null)
            wrapper.eq(ProductPO::getSkuType, skuType.name());
        if (categoryId != null)
            wrapper.eq(ProductPO::getCategoryId, categoryId);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.strip();
            wrapper.and(q -> q.like(ProductPO::getTitle, kw)
                    .or().like(ProductPO::getSlug, kw)
                    .or().like(ProductPO::getBrand, kw));
        }
        if (tag != null && !tag.isBlank())
            wrapper.like(ProductPO::getTags, tag.strip());
        wrapper.orderByDesc(ProductPO::getUpdatedAt);
        return wrapper;
    }

    /**
     * 批量加载商品图库
     *
     * @param productIds 商品 ID 列表
     * @return 商品 ID -> 图库映射
     */
    private Map<Long, List<ProductImagePO>> loadGallery(@NotNull List<Long> productIds) {
        if (productIds.isEmpty())
            return Collections.emptyMap();
        return productImageMapper.selectList(new LambdaQueryWrapper<ProductImagePO>()
                        .in(ProductImagePO::getProductId, productIds)
                        .orderByDesc(ProductImagePO::getIsMain)
                        .orderByAsc(ProductImagePO::getSortOrder)
                        .orderByAsc(ProductImagePO::getId))
                .stream()
                .collect(Collectors.groupingBy(ProductImagePO::getProductId));
    }

    /**
     * 持久化商品图库
     *
     * @param productId 商品 ID
     * @param gallery   图库
     */
    private void persistGallery(@NotNull Long productId, @NotNull List<ProductImage> gallery) {
        if (gallery.isEmpty())
            return;
        for (ProductImage image : gallery) {
            ProductImagePO po = ProductImagePO.builder()
                    .productId(productId)
                    .url(image.getUrl())
                    .isMain(image.isMain())
                    .sortOrder(image.getSortOrder())
                    .createdAt(LocalDateTime.now())
                    .build();
            productImageMapper.insert(po);
        }
    }

    /**
     * 序列化标签 JSON
     *
     * @param tags 标签列表
     * @return JSON 字符串
     */
    private String writeTags(@Nullable List<String> tags) {
        if (tags == null || tags.isEmpty())
            return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 将持久化对象组合成商品聚合
     *
     * @param po 商品持久化对象
     * @return 商品聚合
     */
    private Product buildAggregate(@NotNull ProductPO po) {
        List<String> tags = parseTags(po.getTags());
        List<ProductImage> gallery = toGallery(po.getGallery());
        List<ProductSpec> specs = toSpecs(po.getSpecs());
        List<ProductI18n> i18nList = toProductI18n(po.getI18nList());
        return Product.reconstitute(
                po.getId(), po.getSlug(), po.getTitle(), po.getSubtitle(), po.getDescription(),
                po.getCategoryId(), po.getBrand(), po.getCoverImageUrl(),
                po.getStockTotal() == null ? 0 : po.getStockTotal(),
                po.getSaleCount() == null ? 0 : po.getSaleCount(),
                SkuType.from(po.getSkuType()),
                ProductStatus.from(po.getStatus()),
                po.getDefaultSkuId(), tags, gallery, specs, i18nList,
                po.getCreatedAt(), po.getUpdatedAt()
        );
    }

    /**
     * 解析标签 JSON
     *
     * @param json JSON 串
     * @return 标签列表
     */
    private List<String> parseTags(@Nullable String json) {
        if (json == null || json.isBlank())
            return Collections.emptyList();
        try {
            List<String> parsed = OBJECT_MAPPER.readValue(json, new TypeReference<>() {
            });
            return normalizeTags(parsed);
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private List<ProductImage> toGallery(@Nullable List<ProductImagePO> pos) {
        if (pos == null || pos.isEmpty())
            return Collections.emptyList();
        return pos.stream()
                .filter(Objects::nonNull)
                .map(po -> ProductImage.of(
                        po.getUrl(),
                        Boolean.TRUE.equals(po.getIsMain()),
                        po.getSortOrder() == null ? 0 : po.getSortOrder()
                ))
                .toList();
    }

    private List<ProductSpec> toSpecs(@Nullable List<ProductSpecPO> specPos) {
        if (specPos == null || specPos.isEmpty())
            return Collections.emptyList();
        List<ProductSpec> specs = new ArrayList<>();
        for (ProductSpecPO po : specPos) {
            List<ProductSpecI18n> i18nList = toSpecI18n(po.getI18nList());
            List<ProductSpecValue> values = toSpecValues(po.getValues());
            ProductSpec spec = ProductSpec.reconstitute(
                    po.getId(), po.getProductId(), po.getSpecCode(), po.getSpecName(),
                    SpecType.from(po.getSpecType()),
                    Boolean.TRUE.equals(po.getIsRequired()),
                    po.getSortOrder() == null ? 0 : po.getSortOrder(),
                    "ENABLED".equalsIgnoreCase(po.getStatus()),
                    i18nList, values
            );
            specs.add(spec);
        }
        return specs;
    }

    private List<ProductSpecI18n> toSpecI18n(@Nullable List<ProductSpecI18nPO> pos) {
        if (pos == null || pos.isEmpty())
            return Collections.emptyList();
        return pos.stream()
                .filter(Objects::nonNull)
                .map(po -> ProductSpecI18n.of(normalizeLocale(po.getLocale()), po.getSpecName()))
                .toList();
    }

    private List<ProductSpecValue> toSpecValues(@Nullable List<ProductSpecValuePO> pos) {
        if (pos == null || pos.isEmpty())
            return Collections.emptyList();
        List<ProductSpecValue> values = new ArrayList<>();
        for (ProductSpecValuePO po : pos) {
            Map<String, Object> attributes = parseAttributes(po.getAttributes());
            List<ProductSpecValueI18n> i18nList = toSpecValueI18n(po.getI18nList());
            ProductSpecValue value = ProductSpecValue.reconstitute(
                    po.getId(), po.getProductId(), po.getSpecId(),
                    po.getValueCode(), po.getValueName(), attributes,
                    po.getSortOrder() == null ? 0 : po.getSortOrder(),
                    "ENABLED".equalsIgnoreCase(po.getStatus()),
                    i18nList
            );
            values.add(value);
        }
        return values;
    }

    private List<ProductSpecValueI18n> toSpecValueI18n(@Nullable List<ProductSpecValueI18nPO> pos) {
        if (pos == null || pos.isEmpty())
            return Collections.emptyList();
        return pos.stream()
                .filter(Objects::nonNull)
                .map(po -> ProductSpecValueI18n.of(normalizeLocale(po.getLocale()), po.getValueName()))
                .toList();
    }

    private List<ProductI18n> toProductI18n(@Nullable List<ProductI18nPO> pos) {
        if (pos == null || pos.isEmpty())
            return Collections.emptyList();
        return pos.stream()
                .filter(Objects::nonNull)
                .map(po -> ProductI18n.of(
                        normalizeLocale(po.getLocale()),
                        po.getTitle(),
                        po.getSubtitle(),
                        po.getDescription(),
                        po.getSlug(),
                        parseTags(po.getTags())
                ))
                .toList();
    }

    private Map<String, Object> parseAttributes(@Nullable String json) {
        if (json == null || json.isBlank())
            return Collections.emptyMap();
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {
            });
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }
}
