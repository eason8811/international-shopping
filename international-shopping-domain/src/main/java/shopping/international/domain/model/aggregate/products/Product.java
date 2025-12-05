package shopping.international.domain.model.aggregate.products;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.enums.products.SpecType;
import shopping.international.domain.model.vo.products.ProductI18n;
import shopping.international.domain.model.vo.products.ProductImage;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 商品 SPU 聚合根, 对应表 product
 *
 * <p>职责: 维护基本信息、标签、图集、规格定义、多语言覆盖以及默认 SKU 等聚合内不变式</p>
 */
@Getter
@ToString
@Accessors(chain = true)
public class Product implements Verifiable {
    /**
     * SPU ID (可空表示未持久化)
     */
    private Long id;
    /**
     * 唯一 slug, 用于路由/SEO
     */
    private String slug;
    /**
     * 标题
     */
    private String title;
    /**
     * 副标题
     */
    private String subtitle;
    /**
     * 描述
     */
    private String description;
    /**
     * 分类 ID
     */
    private Long categoryId;
    /**
     * 品牌文案
     */
    private String brand;
    /**
     * 封面图 URL
     */
    private String coverImageUrl;
    /**
     * 聚合库存
     */
    private int stockTotal;
    /**
     * 聚合销量
     */
    private int saleCount;
    /**
     * 规格类型 (单/多规格)
     */
    private SkuType skuType;
    /**
     * 商品状态
     */
    private ProductStatus status;
    /**
     * 默认展示 SKU ID
     */
    private Long defaultSkuId;
    /**
     * 标签列表
     */
    private List<String> tags;
    /**
     * 图库
     */
    private List<ProductImage> gallery;
    /**
     * 规格定义列表, specCode 唯一
     */
    private List<ProductSpec> specs;
    /**
     * 多语言覆盖
     */
    private List<ProductI18n> i18nList;
    /**
     * 创建时间
     */
    private final LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private final LocalDateTime updatedAt;

    /**
     * 私有构造函数
     *
     * @param id            SPU ID
     * @param slug          slug
     * @param title         标题
     * @param subtitle      副标题
     * @param description   描述
     * @param categoryId    分类 ID
     * @param brand         品牌
     * @param coverImageUrl 主图 URL
     * @param stockTotal    聚合库存
     * @param saleCount     聚合销量
     * @param skuType       规格类型
     * @param status        状态
     * @param defaultSkuId  默认 SKU
     * @param tags          标签
     * @param gallery       图库
     * @param specs         规格定义
     * @param i18nList      多语言列表
     * @param createdAt     创建时间
     * @param updatedAt     更新时间
     */
    private Product(Long id, String slug, String title, String subtitle, String description,
                    Long categoryId, String brand, String coverImageUrl,
                    int stockTotal, int saleCount, SkuType skuType, ProductStatus status,
                    Long defaultSkuId, List<String> tags, List<ProductImage> gallery,
                    List<ProductSpec> specs, List<ProductI18n> i18nList,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        requireNotBlank(slug, "商品 slug 不能为空");
        requireNotBlank(title, "商品标题不能为空");
        requireNotNull(categoryId, "分类 ID 不能为空");
        requireNotNull(skuType, "规格类型不能为空");
        requireNotNull(status, "商品状态不能为空");
        this.id = id;
        this.slug = slug.strip();
        this.title = title.strip();
        this.subtitle = subtitle == null ? null : subtitle.strip();
        this.description = description == null ? null : description.strip();
        this.categoryId = categoryId;
        this.brand = brand == null ? null : brand.strip();
        this.coverImageUrl = coverImageUrl == null ? null : coverImageUrl.strip();
        this.stockTotal = stockTotal;
        this.saleCount = saleCount;
        this.skuType = skuType;
        this.status = status;
        this.defaultSkuId = defaultSkuId;
        this.tags = normalizeTags(tags);
        this.gallery = normalizeFieldList(gallery, ProductImage::validate);
        this.specs = normalizeDistinctList(specs, ProductSpec::validate, ProductSpec::getSpecCode, "规格编码不能重复");
        this.i18nList = normalizeDistinctList(i18nList, ProductI18n::validate, ProductI18n::getLocale, "商品多语言 locale 不能重复");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建新的 SPU
     *
     * @param slug          唯一 slug
     * @param title         标题
     * @param subtitle      副标题
     * @param description   描述
     * @param categoryId    分类 ID
     * @param brand         品牌
     * @param coverImageUrl 主图 URL
     * @param skuType       规格类型
     * @param status        商品状态
     * @param tags          标签
     * @param gallery       图库
     * @param specs         规格定义
     * @param i18nList      多语言覆盖
     * @return 新建的商品聚合根
     */
    public static Product create(String slug, String title, String subtitle, String description,
                                 Long categoryId, String brand, String coverImageUrl,
                                 SkuType skuType, ProductStatus status, List<String> tags,
                                 List<ProductImage> gallery, List<ProductSpec> specs, List<ProductI18n> i18nList) {
        return new Product(null, slug, title, subtitle, description, categoryId, brand, coverImageUrl,
                0, 0, skuType == null ? SkuType.SINGLE : skuType,
                status == null ? ProductStatus.DRAFT : status, null, tags, gallery, specs, i18nList,
                LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * 从持久化层重建 SPU
     *
     * @param id            SPU ID
     * @param slug          slug
     * @param title         标题
     * @param subtitle      副标题
     * @param description   描述
     * @param categoryId    分类 ID
     * @param brand         品牌
     * @param coverImageUrl 主图 URL
     * @param stockTotal    聚合库存
     * @param saleCount     聚合销量
     * @param skuType       规格类型
     * @param status        状态
     * @param defaultSkuId  默认 SKU
     * @param tags          标签
     * @param gallery       图库
     * @param specs         规格定义
     * @param i18nList      多语言覆盖
     * @param createdAt     创建时间
     * @param updatedAt     更新时间
     * @return 重建后的商品聚合
     */
    public static Product reconstitute(Long id, String slug, String title, String subtitle, String description,
                                       Long categoryId, String brand, String coverImageUrl,
                                       int stockTotal, int saleCount, SkuType skuType, ProductStatus status,
                                       Long defaultSkuId, List<String> tags, List<ProductImage> gallery,
                                       List<ProductSpec> specs, List<ProductI18n> i18nList,
                                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Product(id, slug, title, subtitle, description, categoryId, brand, coverImageUrl,
                stockTotal, saleCount, skuType, status, defaultSkuId, tags, gallery, specs, i18nList, createdAt, updatedAt);
    }

    /**
     * 更新商品基础信息
     *
     * @param slug          新 slug, 为空则忽略
     * @param title         新标题, 为空则忽略
     * @param subtitle      新副标题, 为空则忽略
     * @param description   新描述, 为空则忽略
     * @param categoryId    新分类 ID, 为空则忽略
     * @param brand         新品牌, 为空则忽略
     * @param coverImageUrl 新主图, 为空则忽略
     * @param skuType       新规格类型, 为空则忽略
     * @param status        新商品状态, 为空则忽略
     * @param tags          新标签列表, 为空则忽略
     */
    public void updateBasic(String slug, String title, String subtitle, String description,
                            Long categoryId, String brand, String coverImageUrl,
                            SkuType skuType, ProductStatus status, List<String> tags) {
        if (slug != null) {
            requireNotBlank(slug, "商品 slug 不能为空");
            this.slug = slug.strip();
        }
        if (title != null) {
            requireNotBlank(title, "商品标题不能为空");
            this.title = title.strip();
        }
        if (subtitle != null)
            this.subtitle = subtitle.strip();
        if (description != null)
            this.description = description.strip();
        if (categoryId != null)
            this.categoryId = categoryId;
        if (brand != null)
            this.brand = brand.strip();
        if (coverImageUrl != null)
            this.coverImageUrl = coverImageUrl.strip();
        if (skuType != null)
            this.skuType = skuType;
        if (status != null)
            changeStatus(status);
        if (tags != null)
            this.tags = normalizeTags(tags);
    }

    /**
     * 新增多语言条目 (locale 不可重复, 标题/slug 必填)
     *
     * @param i18n 新增多语言
     */
    public void addI18n(ProductI18n i18n) {
        requireNotNull(i18n, "商品多语言不能为空");
        i18n.validate();
        List<ProductI18n> mutable = i18nList == null ? new ArrayList<>() : new ArrayList<>(i18nList);
        boolean exists = mutable.stream().anyMatch(item -> item.getLocale().equals(i18n.getLocale()));
        require(!exists, "商品多语言 locale 已存在: " + i18n.getLocale());
        mutable.add(i18n);
        this.i18nList = normalizeDistinctList(mutable, ProductI18n::validate, ProductI18n::getLocale, "商品多语言 locale 不能重复");
    }

    /**
     * 更新已存在的多语言条目 (locale 必须存在, 为空字段不更新)
     *
     * @param locale      语言代码
     * @param title       标题, null 则保留
     * @param subtitle    副标题, null 则保留
     * @param description 描述, null 则保留
     * @param slug        slug, null 则保留
     * @param tags        标签, null 则保留
     */
    public void updateI18n(String locale, String title, String subtitle, String description, String slug, List<String> tags) {
        String normalizedLocale = normalizeLocale(locale);
        requireNotNull(normalizedLocale, "locale 不能为空");
        List<ProductI18n> mutable = i18nList == null ? new ArrayList<>() : new ArrayList<>(i18nList);
        ProductI18n existing = mutable.stream()
                .filter(item -> item.getLocale().equals(normalizedLocale))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("商品多语言不存在: " + normalizedLocale));
        String mergedTitle = title != null ? title.strip() : existing.getTitle();
        String mergedSubtitle = subtitle != null ? subtitle.strip() : existing.getSubtitle();
        String mergedDescription = description != null ? description.strip() : existing.getDescription();
        String mergedSlug = slug != null ? slug.strip() : existing.getSlug();
        List<String> mergedTags = tags != null ? normalizeTags(tags) : existing.getTags();
        requireNotBlank(mergedTitle, "多语言标题不能为空");
        requireNotBlank(mergedSlug, "多语言 slug 不能为空");

        ProductI18n patched = ProductI18n.of(normalizedLocale, mergedTitle, mergedSubtitle, mergedDescription, mergedSlug, mergedTags);
        mutable.removeIf(item -> item.getLocale().equals(normalizedLocale));
        mutable.add(patched);
        this.i18nList = normalizeDistinctList(mutable, ProductI18n::validate, ProductI18n::getLocale, "商品多语言 locale 不能重复");
    }

    /**
     * 替换图库
     *
     * @param gallery 新图库
     */
    public void replaceGallery(List<ProductImage> gallery) {
        this.gallery = normalizeFieldList(gallery, ProductImage::validate);
    }

    /**
     * 替换规格定义
     *
     * @param specs 新规格定义
     */
    public void replaceSpecs(List<ProductSpec> specs) {
        this.specs = normalizeDistinctList(specs, ProductSpec::validate, ProductSpec::getSpecCode, "规格编码不能重复");
    }

    /**
     * 新增规格定义 (specCode 不可重复)
     *
     * @param spec 规格定义
     */
    public void addSpec(ProductSpec spec) {
        requireNotNull(spec, "规格不能为空");
        spec.validate();
        if (this.id != null)
            require(Objects.equals(this.id, spec.getProductId()), "规格所属商品不匹配");
        List<ProductSpec> mutable = specs == null ? new ArrayList<>() : new ArrayList<>(specs);
        boolean exists = mutable.stream().anyMatch(item -> item.getSpecCode().equals(spec.getSpecCode()));
        require(!exists, "规格编码已存在: " + spec.getSpecCode());
        mutable.add(spec);
        this.specs = normalizeDistinctList(mutable, ProductSpec::validate, ProductSpec::getSpecCode, "规格编码不能重复");
    }

    /**
     * 更新已有规格 (按 ID 定位, 为空字段不更新)
     *
     * @param specId    规格 ID
     * @param specName  新名称, null 则保留
     * @param specType  新类型, null 则保留
     * @param required  是否必选, null 则保留
     * @param sortOrder 排序, null 则保留
     * @param enabled   启用状态, null 则保留
     */
    public void updateSpec(Long specId, String specName, SpecType specType, Boolean required, Integer sortOrder, Boolean enabled) {
        requireNotNull(specId, "规格 ID 不能为空");
        if (specs == null)
            throw new IllegalStateException("规格不存在: " + specId);
        List<ProductSpec> mutable = new ArrayList<>(specs);
        ProductSpec existing = mutable.stream()
                .filter(item -> Objects.equals(item.getId(), specId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("规格不存在: " + specId));
        existing.update(specName, specType, required, sortOrder, enabled);
    }

    /**
     * 替换多语言覆盖
     *
     * @param i18nList 新多语言列表
     */
    public void replaceI18n(List<ProductI18n> i18nList) {
        this.i18nList = normalizeDistinctList(i18nList, ProductI18n::validate, ProductI18n::getLocale, "商品多语言 locale 不能重复");
    }

    /**
     * 按合法流转规则更新商品状态
     *
     * @param newStatus 目标状态, 不可为空
     */
    public void changeStatus(ProductStatus newStatus) {
        requireNotNull(newStatus, "商品状态不能为空");
        if (Objects.equals(this.status, newStatus))
            return;
        if (this.status == null) {
            this.status = newStatus;
            return;
        }
        switch (this.status) {
            case DRAFT -> require(newStatus == ProductStatus.ON_SALE || newStatus == ProductStatus.DELETED,
                    "草稿商品仅能上架或删除");
            case ON_SALE -> require(newStatus == ProductStatus.OFF_SHELF || newStatus == ProductStatus.DELETED,
                    "上架商品仅能下架或删除");
            case OFF_SHELF -> require(newStatus == ProductStatus.ON_SALE || newStatus == ProductStatus.DELETED,
                    "下架商品仅能重新上架或删除");
            case DELETED -> throw new IllegalStateException("已删除商品不能再次流转");
            default -> throw new IllegalStateException("未知商品状态: " + this.status);
        }
        this.status = newStatus;
    }

    /**
     * 更新聚合库存与销量
     *
     * @param stockTotal 新库存
     * @param saleCount  新销量
     */
    public void refreshCounters(int stockTotal, int saleCount) {
        require(stockTotal >= 0, "库存不能为负数");
        require(saleCount >= 0, "销量不能为负数");
        this.stockTotal = stockTotal;
        this.saleCount = saleCount;
    }

    /**
     * 设置默认 SKU
     *
     * @param defaultSkuId 默认 SKU ID, 可空表示取消
     */
    public void setDefaultSkuId(Long defaultSkuId) {
        requireNotNull(defaultSkuId, "默认 SKU ID 不能为空");
        this.defaultSkuId = defaultSkuId;
    }

    /**
     * 为商品分配 ID (幂等)
     *
     * @param id 新 ID
     */
    public void assignId(Long id) {
        requireNotNull(id, "商品 ID 不能为空");
        if (this.id != null && !Objects.equals(this.id, id))
            throw new IllegalStateException("商品已存在 ID, 不允许覆盖, current=" + this.id + ", new=" + id);
        this.id = id;
    }

    /**
     * 校验商品聚合根
     */
    @Override
    public void validate() {
        requireNotBlank(slug, "商品 slug 不能为空");
        requireNotBlank(title, "商品标题不能为空");
        requireNotNull(categoryId, "分类 ID 不能为空");
        requireNotNull(skuType, "规格类型不能为空");
        requireNotNull(status, "商品状态不能为空");
    }
}
