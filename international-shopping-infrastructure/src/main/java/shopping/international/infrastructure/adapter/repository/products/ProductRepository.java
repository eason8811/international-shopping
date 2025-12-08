package shopping.international.infrastructure.adapter.repository.products;

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
import shopping.international.infrastructure.dao.products.ProductCategoryMapper;
import shopping.international.infrastructure.dao.products.ProductMapper;
import shopping.international.infrastructure.dao.products.po.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

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
