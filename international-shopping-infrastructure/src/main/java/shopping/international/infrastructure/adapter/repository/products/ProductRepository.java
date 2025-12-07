package shopping.international.infrastructure.adapter.repository.products;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import shopping.international.infrastructure.dao.products.*;
import shopping.international.infrastructure.dao.products.po.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static shopping.international.types.utils.FieldValidateUtils.normalizeLocale;
import static shopping.international.types.utils.FieldValidateUtils.normalizeTags;

/**
 * 基于 MyBatis-Plus 的商品聚合仓储实现
 *
 * <p>负责组合查询商品基础信息、图库、规格/规格值及多语言数据, 并提供默认 SKU 与库存的持久化操作。</p>
 */
@Repository
@RequiredArgsConstructor
public class ProductRepository implements IProductRepository {

    /**
     * 商品主表 Mapper
     */
    private final ProductMapper productMapper;
    /**
     * 商品多语言 Mapper
     */
    private final ProductI18nMapper productI18nMapper;
    /**
     * 商品图片 Mapper
     */
    private final ProductImageMapper productImageMapper;
    /**
     * 规格 Mapper
     */
    private final ProductSpecMapper productSpecMapper;
    /**
     * 规格多语言 Mapper
     */
    private final ProductSpecI18nMapper productSpecI18nMapper;
    /**
     * 规格值 Mapper
     */
    private final ProductSpecValueMapper productSpecValueMapper;
    /**
     * 规格值多语言 Mapper
     */
    private final ProductSpecValueI18nMapper productSpecValueI18nMapper;
    /**
     * 分类 Mapper
     */
    private final ProductCategoryMapper productCategoryMapper;
    /**
     * Jackson 对象映射器, 用于解析 JSON 字段
     */
    private final ObjectMapper OBJECT_MAPPER;

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<Product> findById(@NotNull Long productId) {
        ProductPO po = productMapper.selectById(productId);
        if (po == null)
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
        ProductI18nPO i18n = productI18nMapper.selectOne(new LambdaQueryWrapper<ProductI18nPO>()
                .eq(ProductI18nPO::getSlug, slug)
                .eq(ProductI18nPO::getLocale, locale)
                .last("limit 1"));
        if (i18n != null) {
            ProductPO po = productMapper.selectById(i18n.getProductId());
            if (po != null && ProductStatus.ON_SALE == ProductStatus.from(po.getStatus()))
                return Optional.of(buildAggregate(po));
        }
        ProductPO po = productMapper.selectOne(new LambdaQueryWrapper<ProductPO>()
                .eq(ProductPO::getSlug, slug)
                .last("limit 1"));
        if (po == null || ProductStatus.ON_SALE != ProductStatus.from(po.getStatus()))
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
     * 将持久化对象组合成商品聚合
     *
     * @param po 商品持久化对象
     * @return 商品聚合
     */
    private Product buildAggregate(@NotNull ProductPO po) {
        List<String> tags = parseTags(po.getTags());
        List<ProductImage> gallery = loadGallery(po.getId());
        List<ProductSpec> specs = loadSpecs(po.getId());
        List<ProductI18n> i18nList = loadProductI18n(po.getId());
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
     * 加载商品图库
     *
     * @param productId 商品 ID
     * @return 图库列表
     */
    private List<ProductImage> loadGallery(@NotNull Long productId) {
        List<ProductImagePO> pos = productImageMapper.selectList(new LambdaQueryWrapper<ProductImagePO>()
                .eq(ProductImagePO::getProductId, productId)
                .orderByDesc(ProductImagePO::getIsMain)
                .orderByAsc(ProductImagePO::getSortOrder)
                .orderByAsc(ProductImagePO::getId));
        if (pos == null || pos.isEmpty())
            return Collections.emptyList();
        return pos.stream()
                .map(po -> ProductImage.of(po.getUrl(), Boolean.TRUE.equals(po.getIsMain()), po.getSortOrder() == null ? 0 : po.getSortOrder()))
                .toList();
    }

    /**
     * 加载规格及规格值
     *
     * @param productId 商品 ID
     * @return 规格列表
     */
    private List<ProductSpec> loadSpecs(@NotNull Long productId) {
        List<ProductSpecPO> specPos = productSpecMapper.selectList(new LambdaQueryWrapper<ProductSpecPO>()
                .eq(ProductSpecPO::getProductId, productId)
                .orderByAsc(ProductSpecPO::getSortOrder)
                .orderByAsc(ProductSpecPO::getId));
        if (specPos == null || specPos.isEmpty())
            return Collections.emptyList();
        Map<Long, List<ProductSpecI18n>> specI18nMap = loadSpecI18n(specPos.stream().map(ProductSpecPO::getId).toList());
        Map<Long, List<ProductSpecValue>> valueMap = loadSpecValues(specPos.stream().map(ProductSpecPO::getId).toList(), productId);
        List<ProductSpec> specs = new ArrayList<>();
        for (ProductSpecPO po : specPos) {
            List<ProductSpecI18n> i18nList = specI18nMap.getOrDefault(po.getId(), Collections.emptyList());
            List<ProductSpecValue> values = valueMap.getOrDefault(po.getId(), Collections.emptyList());
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
     * 加载规格多语言
     *
     * @param specIds 规格 ID 列表
     * @return 规格 ID -> 多语言列表映射
     */
    private Map<Long, List<ProductSpecI18n>> loadSpecI18n(@NotNull List<Long> specIds) {
        if (specIds.isEmpty())
            return Collections.emptyMap();
        List<ProductSpecI18nPO> pos = productSpecI18nMapper.selectList(new LambdaQueryWrapper<ProductSpecI18nPO>()
                .in(ProductSpecI18nPO::getSpecId, specIds));
        Map<Long, List<ProductSpecI18n>> map = new HashMap<>();
        for (ProductSpecI18nPO po : pos) {
            ProductSpecI18n i18n = ProductSpecI18n.of(normalizeLocale(po.getLocale()), po.getSpecName());
            map.computeIfAbsent(po.getSpecId(), k -> new ArrayList<>()).add(i18n);
        }
        return map;
    }

    /**
     * 加载规格值及其多语言
     *
     * @param specIds   规格 ID 列表
     * @param productId 商品 ID
     * @return 规格 ID -> 规格值列表映射
     */
    private Map<Long, List<ProductSpecValue>> loadSpecValues(@NotNull List<Long> specIds, @NotNull Long productId) {
        if (specIds.isEmpty())
            return Collections.emptyMap();
        List<ProductSpecValuePO> valuePos = productSpecValueMapper.selectList(new LambdaQueryWrapper<ProductSpecValuePO>()
                .eq(ProductSpecValuePO::getProductId, productId)
                .in(ProductSpecValuePO::getSpecId, specIds)
                .orderByAsc(ProductSpecValuePO::getSortOrder)
                .orderByAsc(ProductSpecValuePO::getId));
        if (valuePos == null || valuePos.isEmpty())
            return Collections.emptyMap();
        Map<Long, List<ProductSpecValueI18n>> valueI18nMap = loadSpecValueI18n(valuePos.stream().map(ProductSpecValuePO::getId).toList());
        Map<Long, List<ProductSpecValue>> map = new HashMap<>();
        for (ProductSpecValuePO po : valuePos) {
            Map<String, Object> attributes = parseAttributes(po.getAttributes());
            List<ProductSpecValueI18n> i18nList = valueI18nMap.getOrDefault(po.getId(), Collections.emptyList());
            ProductSpecValue value = ProductSpecValue.reconstitute(
                    po.getId(), po.getProductId(), po.getSpecId(),
                    po.getValueCode(), po.getValueName(), attributes,
                    po.getSortOrder() == null ? 0 : po.getSortOrder(),
                    "ENABLED".equalsIgnoreCase(po.getStatus()),
                    i18nList
            );
            map.computeIfAbsent(po.getSpecId(), k -> new ArrayList<>()).add(value);
        }
        return map;
    }

    /**
     * 解析规格值属性 JSON
     *
     * @param json JSON 串
     * @return 属性映射
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
     * 加载规格值多语言
     *
     * @param valueIds 规格值 ID 列表
     * @return 规格值 ID -> 多语言列表
     */
    private Map<Long, List<ProductSpecValueI18n>> loadSpecValueI18n(@NotNull List<Long> valueIds) {
        if (valueIds.isEmpty())
            return Collections.emptyMap();
        List<ProductSpecValueI18nPO> pos = productSpecValueI18nMapper.selectList(new LambdaQueryWrapper<ProductSpecValueI18nPO>()
                .in(ProductSpecValueI18nPO::getValueId, valueIds));
        Map<Long, List<ProductSpecValueI18n>> map = new HashMap<>();
        for (ProductSpecValueI18nPO po : pos) {
            ProductSpecValueI18n i18n = ProductSpecValueI18n.of(normalizeLocale(po.getLocale()), po.getValueName());
            map.computeIfAbsent(po.getValueId(), k -> new ArrayList<>()).add(i18n);
        }
        return map;
    }

    /**
     * 加载商品多语言
     *
     * @param productId 商品 ID
     * @return 多语言列表
     */
    private List<ProductI18n> loadProductI18n(@NotNull Long productId) {
        List<ProductI18nPO> pos = productI18nMapper.selectList(new LambdaQueryWrapper<ProductI18nPO>()
                .eq(ProductI18nPO::getProductId, productId));
        if (pos == null || pos.isEmpty())
            return Collections.emptyList();
        return pos.stream()
                .map(po ->
                        ProductI18n.of(
                                normalizeLocale(po.getLocale()),
                                po.getTitle(),
                                po.getSubtitle(),
                                po.getDescription(),
                                po.getSlug(),
                                parseTags(po.getTags())
                        )
                )
                .collect(Collectors.toList());
    }
}
