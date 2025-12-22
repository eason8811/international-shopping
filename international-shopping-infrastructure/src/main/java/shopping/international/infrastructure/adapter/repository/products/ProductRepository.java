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
import shopping.international.domain.model.vo.products.*;
import shopping.international.infrastructure.dao.products.ProductCategoryMapper;
import shopping.international.infrastructure.dao.products.ProductI18nMapper;
import shopping.international.infrastructure.dao.products.ProductImageMapper;
import shopping.international.infrastructure.dao.products.ProductMapper;
import shopping.international.infrastructure.dao.products.po.*;
import shopping.international.types.exceptions.ConflictException;

import java.io.IOException;
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
     * 商品分类 Repository
     */
    private final CategoryRepository categoryRepository;
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
     * 分页查询上架商品列表
     *
     * @param criteria 检索条件
     * @param offset   偏移量, 从 0 开始
     * @param limit    单页条数
     * @return 商品快照列表
     */
    @Override
    public @NotNull List<ProductPublicSnapshot> pageOnSale(@NotNull ProductSearchCriteria criteria, int offset, int limit) {
        List<PublicProductSnapshotPO> pos = productMapper.selectPublicList(
                criteria.getLocale(),
                criteria.getCurrency(),
                criteria.getCategorySlug(),
                criteria.getKeyword(),
                criteria.getTags(),
                criteria.getPriceMin(),
                criteria.getPriceMax(),
                criteria.getSort().name(),
                offset,
                limit
        );
        return buildSnapshots(pos, criteria.getCurrency());
    }

    /**
     * 统计上架商品数量
     *
     * @param criteria 检索条件
     * @return 满足条件的总数
     */
    @Override
    public long countOnSale(@NotNull ProductSearchCriteria criteria) {
        Long count = productMapper.countPublicList(
                criteria.getLocale(),
                criteria.getCurrency(),
                criteria.getCategorySlug(),
                criteria.getKeyword(),
                criteria.getTags(),
                criteria.getPriceMin(),
                criteria.getPriceMax()
        );
        return count == null ? 0 : count;
    }

    /**
     * 分页查询用户点赞的商品
     *
     * @param userId   用户 ID
     * @param criteria 检索条件(主要用于 locale/currency)
     * @param offset   偏移量, 从 0 开始
     * @param limit    单页条数
     * @return 点赞的商品快照列表
     */
    @Override
    public @NotNull List<ProductPublicSnapshot> pageUserLikes(@NotNull Long userId, @NotNull ProductSearchCriteria criteria, int offset, int limit) {
        List<PublicProductSnapshotPO> pos = productMapper.selectUserLikedList(userId, criteria.getLocale(),
                criteria.getCurrency(), offset, limit);
        return buildSnapshots(pos, criteria.getCurrency());
    }

    /**
     * 统计用户点赞的商品数量
     *
     * @param userId   用户 ID
     * @param criteria 检索条件
     * @return 点赞商品总数
     */
    @Override
    public long countUserLikes(@NotNull Long userId, @NotNull ProductSearchCriteria criteria) {
        Long count = productMapper.countUserLikedList(userId, criteria.getLocale(), criteria.getCurrency());
        return count == null ? 0 : count;
    }

    /**
     * 更新商品的默认 SKU ID
     *
     * @param productId    商品 ID
     * @param defaultSkuId 默认 SKU ID, 可为空
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDefaultSkuId(@NotNull Long productId, @Nullable Long defaultSkuId) {
        LambdaUpdateWrapper<ProductPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ProductPO::getId, productId)
                .set(ProductPO::getDefaultSkuId, defaultSkuId);
        productMapper.update(null, wrapper);
    }

    /**
     * 覆盖更新商品聚合库存
     *
     * @param productId 商品 ID
     * @param stock     新聚合库存
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockTotal(@NotNull Long productId, int stock) {
        LambdaUpdateWrapper<ProductPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ProductPO::getId, productId)
                .set(ProductPO::getStockTotal, stock);
        productMapper.update(null, wrapper);
    }

    /**
     * 新增商品聚合 (含基础信息与可选图库)
     *
     * @param product 待保存的商品聚合, ID 为空
     * @return 保存后的商品聚合, 携带持久化 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull Product save(@NotNull Product product) {
        categoryRepository.findById(product.getCategoryId())
                .orElseThrow(() -> new ConflictException("分类不存在"));
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
     * 增量更新商品基础信息
     *
     * @param product        已存在的商品聚合快照
     * @param replaceGallery 是否需要替换图库
     * @return 更新后的商品聚合
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
                .set(ProductPO::getTags, writeTags(product.getTags()));
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
     * 覆盖商品图库
     *
     * @param productId 商品 ID
     * @param gallery   新图库列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceGallery(@NotNull Long productId, @NotNull List<ProductImage> gallery) {
        productImageMapper.delete(new LambdaQueryWrapper<ProductImagePO>().eq(ProductImagePO::getProductId, productId));
        persistGallery(productId, gallery);
    }

    /**
     * 新增一条商品多语言记录
     *
     * @param productId 商品 ID
     * @param i18n      多语言值对象
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
                .build();
        try {
            productI18nMapper.insert(po);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("商品多语言唯一约束冲突", e);
        }
    }

    /**
     * 更新已存在的商品多语言记录
     *
     * @param productId 商品 ID
     * @param i18n      多语言值对象
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
                .set(ProductI18nPO::getTags, writeTags(i18n.getTags()));
        productI18nMapper.update(null, wrapper);
    }

    /**
     * 删除指定商品的特定语言版本信息
     *
     * @param productId 商品 ID, 用于定位要删除多语言信息的商品
     * @param locale    语言标识, 指定要删除的具体语言版本
     */
    @Override
    public void deleteI18n(@NotNull Long productId, @NotNull String locale) {
        productI18nMapper.delete(new LambdaQueryWrapper<ProductI18nPO>()
                .eq(ProductI18nPO::getProductId, productId)
                .eq(ProductI18nPO::getLocale, normalizeLocale(locale)));
    }

    /**
     * 分页查询商品基础信息
     *
     * @param status         商品状态过滤, 可空
     * @param skuType        SKU 类型过滤, 可空
     * @param categoryId     分类过滤, 可空
     * @param keyword        关键词过滤, 支持标题/slug/品牌, 可空
     * @param tag            标签过滤, 可空
     * @param includeDeleted 是否包含已删除商品
     * @param offset         偏移量, 从 0 开始
     * @param limit          单页大小
     * @return 商品列表
     */
    @Override
    public @NotNull List<Product> list(@Nullable ProductStatus status, @Nullable SkuType skuType,
                                       @Nullable Long categoryId, @Nullable String keyword, @Nullable String tag,
                                       boolean includeDeleted, int offset, int limit) {
        List<ProductPO> pos = productMapper.selectAdminAggregatePage(status, skuType, categoryId, keyword, tag, includeDeleted, offset, limit);
        if (pos == null || pos.isEmpty())
            return Collections.emptyList();
        Map<Long, List<ProductImagePO>> galleryMap = pos.stream()
                .map(ProductPO::getGallery)
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(ProductImagePO::getProductId));
        List<Product> result = new ArrayList<>();
        for (ProductPO po : pos) {
            List<ProductImagePO> imagePOs = galleryMap.getOrDefault(po.getId(), Collections.emptyList());
            List<ProductImage> gallery = toGallery(imagePOs);
            List<String> tags = parseTags(po.getTags());
            result.add(Product.reconstitute(
                    po.getId(),
                    po.getSlug(),
                    po.getTitle(),
                    po.getSubtitle(),
                    po.getDescription(),
                    po.getCategoryId(),
                    po.getBrand(),
                    po.getCoverImageUrl(),
                    po.getStockTotal() == null ? 0 : po.getStockTotal(),
                    po.getSaleCount() == null ? 0 : po.getSaleCount(),
                    SkuType.from(po.getSkuType()),
                    ProductStatus.from(po.getStatus()),
                    po.getDefaultSkuId(),
                    tags,
                    gallery,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    po.getCreatedAt(),
                    po.getUpdatedAt()
            ));
        }
        return result;
    }

    /**
     * 统计分页查询的商品总数
     *
     * @param status         商品状态过滤, 可空
     * @param skuType        SKU 类型过滤, 可空
     * @param categoryId     分类过滤, 可空
     * @param keyword        关键词过滤, 可空
     * @param tag            标签过滤, 可空
     * @param includeDeleted 是否包含已删除商品
     * @return 总数量
     */
    @Override
    public long count(@Nullable ProductStatus status, @Nullable SkuType skuType,
                      @Nullable Long categoryId, @Nullable String keyword, @Nullable String tag, boolean includeDeleted) {
        Long total = productMapper.countAdminAggregatePage(status, skuType, categoryId, keyword, tag, includeDeleted);
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
                .build();
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

    /**
     * 将商品图片持久化对象列表转换为商品图片值对象列表
     *
     * @param pos 商品图片持久化对象列表, 可为空或空列表
     * @return 转换后的商品图片值对象列表, 若输入为空或空列表则返回空列表
     */
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

    /**
     * 将商品规格持久化对象列表转换为商品规格值对象列表
     *
     * @param specPos 商品规格持久化对象列表, 可为空或空列表
     * @return 转换后的商品规格值对象列表, 若输入为空或空列表则返回空列表
     */
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

    /**
     * 将规格类别多语言持久化对象列表转换为规格类别多语言值对象列表
     *
     * <p>此方法接收一个 {@code ProductSpecI18nPO} 对象列表, 并将其转换为 {@link ProductSpecI18n} 对象列表, 如果输入为空或空列表,
     * 则返回空列表</p>
     *
     * @param pos 规格类别多语言持久化对象列表, 可为空或空列表
     * @return 转换后的规格类别多语言值对象列表, 若输入为空或空列表则返回空列表
     */
    private List<ProductSpecI18n> toSpecI18n(@Nullable List<ProductSpecI18nPO> pos) {
        if (pos == null || pos.isEmpty())
            return Collections.emptyList();
        return pos.stream()
                .filter(Objects::nonNull)
                .map(po -> ProductSpecI18n.of(normalizeLocale(po.getLocale()), po.getSpecName()))
                .toList();
    }

    /**
     * 将商品规格值持久化对象列表转换为商品规格值对象列表
     *
     * @param pos 商品规格值持久化对象列表, 可为空或空列表
     * @return 转换后的商品规格值对象列表, 若输入为空或空列表则返回空列表
     */
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

    /**
     * 将规格值多语言持久化对象列表转换为规格值多语言值对象列表
     *
     * <p>此方法接收一个 {@code ProductSpecValueI18nPO} 对象列表, 并将其转换为 {@link ProductSpecValueI18n} 对象列表,
     * 如果输入为空或空列表, 则返回空列表</p>
     *
     * @param pos 规格值多语言持久化对象列表, 可为空或空列表
     * @return 转换后的规格值多语言值对象列表, 若输入为空或空列表则返回空列表
     */
    private List<ProductSpecValueI18n> toSpecValueI18n(@Nullable List<ProductSpecValueI18nPO> pos) {
        if (pos == null || pos.isEmpty())
            return Collections.emptyList();
        return pos.stream()
                .filter(Objects::nonNull)
                .map(po -> ProductSpecValueI18n.of(normalizeLocale(po.getLocale()), po.getValueName()))
                .toList();
    }

    /**
     * 将商品多语言持久化对象列表转换为商品多语言值对象列表
     *
     * @param pos 商品多语言持久化对象列表, 可为空或空列表
     * @return 转换后的商品多语言值对象列表, 若输入为空或空列表则返回空列表
     */
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

    /**
     * 解析 JSON 字符串为属性映射
     *
     * <p>此方法尝试将给定的 JSON 字符串解析为一个键值对映射, 其中键为字符串类型, 值可以是任何对象类型,
     * 如果输入的 JSON 字符串为空或空白, 则返回一个空映射. 在解析过程中如果遇到异常, 也将返回一个空映射</p>
     *
     * @param json 待解析的 JSON 字符串, 可以为空
     * @return 解析后的属性映射, 如果解析失败则返回空映射
     */
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

    /**
     * 将数据库快照转换为领域视图
     *
     * @param pos      快照列表
     * @param currency 币种
     * @return 领域视图列表
     */
    private List<ProductPublicSnapshot> buildSnapshots(List<PublicProductSnapshotPO> pos, String currency) {
        if (pos == null || pos.isEmpty())
            return Collections.emptyList();
        Map<Long, List<ProductImagePO>> galleryMap = pos.stream()
                .map(PublicProductSnapshotPO::getGallery)
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(ProductImagePO::getProductId));
        List<ProductPublicSnapshot> snapshots = new ArrayList<>();
        for (PublicProductSnapshotPO po : pos) {
            List<ProductImage> gallery = toGallery(galleryMap.getOrDefault(po.getId(), Collections.emptyList()));
            List<String> tags = parseTags(po.getTags());
            ProductPublicSnapshot snapshot = ProductPublicSnapshot.builder()
                    .id(po.getId())
                    .slug(po.getSlug())
                    .title(po.getTitle())
                    .subtitle(po.getSubtitle())
                    .description(po.getDescription())
                    .categoryId(po.getCategoryId())
                    .categorySlug(po.getCategorySlug())
                    .brand(po.getBrand())
                    .coverImageUrl(po.getCoverImageUrl())
                    .stockTotal(po.getStockTotal() == null ? 0 : po.getStockTotal())
                    .saleCount(po.getSaleCount() == null ? 0 : po.getSaleCount())
                    .skuType(SkuType.from(po.getSkuType()))
                    .status(ProductStatus.from(po.getStatus()))
                    .tags(tags)
                    .gallery(gallery)
                    .priceRange(ProductPublicSnapshot.ProductPriceRangeView.builder()
                            .currency(currency)
                            .listPriceMin(po.getListPriceMin())
                            .listPriceMax(po.getListPriceMax())
                            .salePriceMin(po.getSalePriceMin())
                            .salePriceMax(po.getSalePriceMax())
                            .build())
                    .likedAt(po.getLikedAt())
                    .build();
            snapshot.validate();
            snapshots.add(snapshot);
        }
        return snapshots;
    }
}
