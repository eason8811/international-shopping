package shopping.international.infrastructure.adapter.repository.products;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.repository.products.IProductAdminRepository;
import shopping.international.domain.model.entity.products.Product;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.vo.products.ProductI18n;
import shopping.international.domain.model.vo.products.ProductImage;
import shopping.international.infrastructure.dao.products.ProductI18nMapper;
import shopping.international.infrastructure.dao.products.ProductImageMapper;
import shopping.international.infrastructure.dao.products.ProductMapper;
import shopping.international.infrastructure.dao.products.po.ProductI18nPO;
import shopping.international.infrastructure.dao.products.po.ProductImagePO;
import shopping.international.infrastructure.dao.products.po.ProductPO;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品管理仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductAdminRepository implements IProductAdminRepository {
    /**
     * JSON 序列化工具
     */
    private final ObjectMapper objectMapper;
    /**
     * 商品 Mapper
     */
    private final ProductMapper productMapper;
    /**
     * 商品 i18n Mapper
     */
    private final ProductI18nMapper productI18nMapper;
    /**
     * 商品图片 Mapper
     */
    private final ProductImageMapper productImageMapper;

    /**
     * 分页查询商品
     *
     * @param page           页码
     * @param size           每页大小
     * @param status         状态过滤
     * @param skuType        SKU 类型
     * @param categoryId     分类 ID
     * @param keyword        关键词
     * @param tag            标签
     * @param includeDeleted 是否包含已删除
     * @return 分页结果
     */
    @Override
    public @NotNull PageResult<Product> page(int page, int size, String status, String skuType, Long categoryId, String keyword, String tag, boolean includeDeleted) {
        long total = productMapper.countAdminPage(status, skuType, categoryId, keyword, tag, includeDeleted);
        if (total == 0)
            return new PageResult<>(List.of(), 0);
        int offset = Math.max(page - 1, 0) * size;
        List<ProductPO> records = productMapper.selectAdminPage(status, skuType, categoryId, keyword, tag, includeDeleted, size, offset);
        List<Product> items = records.stream().map(this::toEntity).toList();
        return new PageResult<>(items, total);
    }

    /**
     * 按 ID 查询商品
     *
     * @param productId 商品 ID
     * @return 商品
     */
    @Override
    public @NotNull Optional<Product> findById(@NotNull Long productId) {
        ProductPO po = productMapper.selectById(productId);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    /**
     * 批量查询商品
     *
     * @param productIds 商品 ID 集合
     * @return 商品映射
     */
    @Override
    public @NotNull Map<Long, Product> mapByIds(@NotNull Set<Long> productIds) {
        if (productIds.isEmpty())
            return Map.of();
        List<ProductPO> records = productMapper.selectByIds(productIds);
        return records.stream()
                .map(this::toEntity)
                .collect(Collectors.toMap(Product::getId, Function.identity(),
                        (existing, ignore) -> existing, LinkedHashMap::new));
    }

    /**
     * 批量查询商品多语言
     *
     * @param productIds 商品 ID 集合
     * @return productId -> 多语言列表
     */
    @Override
    public @NotNull Map<Long, List<ProductI18n>> mapI18n(@NotNull Collection<Long> productIds) {
        if (productIds.isEmpty())
            return Map.of();
        List<ProductI18nPO> records = productI18nMapper.selectList(new LambdaQueryWrapper<ProductI18nPO>()
                .in(ProductI18nPO::getProductId, productIds)
                .orderByAsc(ProductI18nPO::getProductId, ProductI18nPO::getLocale));
        Map<Long, List<ProductI18n>> result = new LinkedHashMap<>();
        for (ProductI18nPO po : records) {
            result.computeIfAbsent(po.getProductId(), ignore -> new ArrayList<>())
                    .add(toProductI18n(po));
        }
        return result;
    }

    /**
     * 获取商品图库
     *
     * @param productId 商品 ID
     * @return 图库列表
     */
    @Override
    public @NotNull List<ProductImage> listGallery(@NotNull Long productId) {
        List<ProductImagePO> records = productImageMapper.selectList(new LambdaQueryWrapper<ProductImagePO>()
                .eq(ProductImagePO::getProductId, productId)
                .orderByAsc(ProductImagePO::getSortOrder, ProductImagePO::getId));
        return records.stream().map(this::toProductImage).toList();
    }

    /**
     * 批量获取商品图库
     *
     * @param productIds 商品 ID
     * @return productId -> 图库
     */
    @Override
    public @NotNull Map<Long, List<ProductImage>> mapGallery(@NotNull Collection<Long> productIds) {
        if (productIds.isEmpty())
            return Map.of();
        List<ProductImagePO> records = productImageMapper.selectList(new LambdaQueryWrapper<ProductImagePO>()
                .in(ProductImagePO::getProductId, productIds)
                .orderByAsc(ProductImagePO::getSortOrder, ProductImagePO::getId));
        return records.stream()
                .collect(Collectors.groupingBy(ProductImagePO::getProductId,
                        LinkedHashMap::new,
                        Collectors.mapping(this::toProductImage, Collectors.toList())));
    }

    /**
     * 新增商品
     *
     * @param product 商品实体
     * @return 生成的主键 ID
     */
    @Override
    public @NotNull Long insert(@NotNull Product product) {
        ProductPO po = toPo(product);
        productMapper.insert(po);
        return po.getId();
    }

    /**
     * 更新商品
     *
     * @param product 商品实体
     */
    @Override
    public void update(@NotNull Product product) {
        productMapper.updateById(toPo(product));
    }

    /**
     * 批量 upsert 商品多语言
     *
     * @param productId 商品 ID
     * @param payloads  多语言列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upsertI18n(@NotNull Long productId, @NotNull List<ProductI18n> payloads) {
        if (payloads.isEmpty())
            return;
        // 根据 productId 获取 locale -> i18n PO 映射
        Map<String, ProductI18nPO> existingMap = productI18nMapper.selectList(new LambdaQueryWrapper<ProductI18nPO>()
                        .eq(ProductI18nPO::getProductId, productId))
                .stream()
                .collect(Collectors.toMap(ProductI18nPO::getLocale, Function.identity(),
                        (current, ignore) -> current, LinkedHashMap::new));
        for (ProductI18n vo : payloads) {
            ProductI18nPO po = existingMap.get(vo.getLocale());
            if (po == null) {
                productI18nMapper.insert(ProductI18nPO.builder()
                        .productId(productId)
                        .locale(vo.getLocale())
                        .title(vo.getTitle())
                        .subtitle(vo.getSubtitle())
                        .description(vo.getDescription())
                        .slug(vo.getSlug())
                        .tags(toJson(vo.getTags()))
                        .build());
                continue;
            }
            po.setTitle(vo.getTitle());
            po.setSubtitle(vo.getSubtitle());
            po.setDescription(vo.getDescription());
            po.setSlug(vo.getSlug());
            po.setTags(toJson(vo.getTags()));
            productI18nMapper.updateById(po);
        }
    }

    /**
     * 覆盖商品图库
     *
     * @param productId 商品 ID
     * @param gallery   图库列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceGallery(@NotNull Long productId, @NotNull List<ProductImage> gallery) {
        productImageMapper.delete(new LambdaQueryWrapper<ProductImagePO>()
                .eq(ProductImagePO::getProductId, productId));
        if (gallery.isEmpty())
            return;
        for (ProductImage image : gallery) {
            productImageMapper.insert(ProductImagePO.builder()
                    .productId(productId)
                    .url(image.getUrl())
                    .isMain(image.isMain() ? 1 : 0)
                    .sortOrder(image.getSortOrder())
                    .build());
        }
    }

    /**
     * 判断基础 slug 是否重复
     *
     * @param slug      slug
     * @param excludeId 排除 ID
     * @return 是否存在
     */
    @Override
    public boolean existsSlug(@NotNull String slug, Long excludeId) {
        return productMapper.selectCount(new LambdaQueryWrapper<ProductPO>()
                .eq(ProductPO::getSlug, slug)
                .ne(excludeId != null, ProductPO::getId, excludeId)) > 0;
    }

    /**
     * 判断 locale + slug 是否冲突
     *
     * @param locale           语言
     * @param slug             slug
     * @param excludeProductId 排除商品 ID
     * @return 是否存在
     */
    @Override
    public boolean existsLocalizedSlug(@NotNull String locale, @NotNull String slug, Long excludeProductId) {
        return productI18nMapper.selectCount(new LambdaQueryWrapper<ProductI18nPO>()
                .eq(ProductI18nPO::getLocale, locale)
                .eq(ProductI18nPO::getSlug, slug)
                .ne(excludeProductId != null, ProductI18nPO::getProductId, excludeProductId)) > 0;
    }

    /**
     * 将 ProductPO 转换为领域实体
     *
     * @param po 数据库持久化对象
     * @return 商品实体
     */
    private Product toEntity(ProductPO po) {
        return Product.reconstitute(
                po.getId(),
                po.getSlug(),
                po.getTitle(),
                po.getSubtitle(),
                po.getDescription(),
                po.getCategoryId(),
                po.getBrand(),
                po.getCoverImageUrl(),
                po.getStockTotal(),
                po.getSaleCount(),
                SkuType.from(po.getSkuType()),
                ProductStatus.from(po.getStatus()),
                po.getDefaultSkuId(),
                parseStringList(po.getTags()),
                po.getUpdatedAt()
        );
    }

    /**
     * 将领域实体转换为持久化对象
     *
     * @param entity 商品实体
     * @return 持久化对象
     */
    private ProductPO toPo(Product entity) {
        return ProductPO.builder()
                .id(entity.getId())
                .slug(entity.getSlug())
                .title(entity.getTitle())
                .subtitle(entity.getSubtitle())
                .description(entity.getDescription())
                .categoryId(entity.getCategoryId())
                .brand(entity.getBrand())
                .coverImageUrl(entity.getCoverImageUrl())
                .stockTotal(entity.getStockTotal())
                .saleCount(entity.getSaleCount())
                .skuType(entity.getSkuType() == null ? null : entity.getSkuType().name())
                .status(entity.getStatus() == null ? null : entity.getStatus().name())
                .defaultSkuId(entity.getDefaultSkuId())
                .tags(toJson(entity.getTags()))
                .build();
    }

    /**
     * 将 ProductImagePO 转换为图片 VO
     *
     * @param po 持久化对象
     * @return 图片 VO
     */
    private ProductImage toProductImage(ProductImagePO po) {
        return ProductImage.of(po.getUrl(), po.getIsMain() != null && po.getIsMain() == 1, po.getSortOrder());
    }

    /**
     * 将 ProductI18nPO 转换为 i18n VO
     *
     * @param po 持久化对象
     * @return i18n VO
     */
    private ProductI18n toProductI18n(ProductI18nPO po) {
        return ProductI18n.of(po.getLocale(), po.getTitle(), po.getSubtitle(), po.getDescription(),
                po.getSlug(), parseStringList(po.getTags()));
    }

    /**
     * 将标签列表转换为 JSON
     *
     * @param tags 标签列表
     * @return JSON 字符串或 null
     */
    private String toJson(@Nullable List<String> tags) {
        if (tags == null || tags.isEmpty())
            return null;
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (Exception ex) {
            log.warn("标签序列化失败, 使用空列表代替", ex);
            return "[]";
        }
    }

    /**
     * 解析 JSON 为字符串列表
     *
     * @param json JSON 字符串
     * @return 字符串列表
     */
    private List<String> parseStringList(@Nullable String json) {
        if (json == null || json.isBlank())
            return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            log.warn("标签解析失败, 返回空列表, 原始数据: {}", json, ex);
            return Collections.emptyList();
        }
    }
}
