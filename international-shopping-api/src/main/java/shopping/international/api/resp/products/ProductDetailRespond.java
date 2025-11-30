package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.entity.products.ProductSku;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.enums.products.SpecType;
import shopping.international.domain.model.vo.products.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品详情响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailRespond {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 商品别名(SEO/路由)
     */
    private String slug;
    /**
     * 商品标题
     */
    private String title;
    /**
     * 副标题
     */
    private String subtitle;
    /**
     * 商品描述
     */
    private String description;
    /**
     * 所属分类ID
     */
    private Long categoryId;
    /**
     * 分类别名(SEO/路由)
     */
    private String categorySlug;
    /**
     * 品牌文案(本地化)
     */
    private String brand;
    /**
     * 主图URL
     */
    private String coverImageUrl;
    /**
     * 总库存(聚合)
     */
    private Integer stockTotal;
    /**
     * 销量(聚合)
     */
    private Integer saleCount;
    /**
     * 规格类型(单/多规格)
     * <ul>
     *     <li>{@code SINGLE}: 单规格</li>
     *     <li>{@code VARIANT}: 多规格</li>
     * </ul>
     */
    private SkuType skuType;
    /**
     * 商品状态
     * <ul>
     *     <li>{@code DRAFT}: 草稿</li>
     *     <li>{@code ON_SALE}: 上架</li>
     *     <li>{@code OFF_SHELF}: 下架</li>
     *     <li>{@code DELETED}: 已删除</li>
     * </ul>
     */
    private ProductStatus status;
    /**
     * 标签(JSON)
     */
    private List<String> tags;
    /**
     * 默认展示SKU
     */
    private Long defaultSkuId;
    /**
     * SPU 图片列表
     */
    private List<ProductImageRespond> gallery;
    /**
     * SPU 的规格列表
     */
    private List<SpecRespond> specs;
    /**
     * SPU 的 SKU 列表
     */
    private List<SkuRespond> skus;
    /**
     * SPU I18N, 可能为单个对象或多语言数组
     */
    private Object i18n;

    /**
     * 从 <code>ProductDetail</code> 对象和指定的地区代码创建 <code>ProductDetailRespond</code> 实例
     *
     * @param detail <code>ProductDetail</code> 对象, 包含商品详情信息
     * @param locale 地区代码, 用于处理多语言内容
     * @return <code>ProductDetailRespond</code> 实例, 包含转换后的商品详情响应信息
     */
    public static ProductDetailRespond from(ProductDetail detail, String locale) {
        // 从商品详情值对象中获取商品的图片列表, 并转换为 ProductImageRespond 列表
        List<ProductImageRespond> gallery = detail.gallery() == null ? List.of()
                : detail.gallery()
                .stream()
                .map(ProductImageRespond::from)
                .toList();
        // 从商品详情值对象中获取商品的规格列表, 并转换为 本地化后的 SpecRespond 列表
        List<SpecRespond> specResponds = detail.specs() == null ? List.of()
                : detail.specs()
                .stream()
                .map(spec -> SpecRespond.from(spec, locale))
                .toList();
        // 从商品详情值对象中获取商品的 SKU 列表, 并转换为 SkuRespond 列表
        List<SkuRespond> skuResponds = detail.skus() == null ? List.of()
                : detail.skus()
                .stream()
                .map(SkuRespond::from)
                .toList();
        // 从商品详情值对象中获取商品的 I18N 信息, 并转换为 ProductI18nRespond
        Object i18nRespond;
        if (detail.i18nList() != null && !detail.i18nList().isEmpty()) {
            i18nRespond = detail.i18nList().stream()
                    .map(ProductI18nRespond::from)
                    .toList();
        } else
            i18nRespond = detail.i18n() == null ? null : ProductI18nRespond.from(detail.i18n());
        return new ProductDetailRespond(
                detail.id(),
                detail.slug(),
                detail.title(),
                detail.subtitle(),
                detail.description(),
                detail.categoryId(),
                detail.categorySlug(),
                detail.brand(),
                detail.coverImageUrl(),
                detail.stockTotal(),
                detail.saleCount(),
                detail.skuType(),
                detail.status(),
                detail.tags(),
                detail.defaultSkuId(),
                gallery,
                specResponds,
                skuResponds,
                i18nRespond
        );
    }

    /**
     * 规格响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecRespond {
        /**
         * 规格 ID
         */
        private Long specId;
        /**
         * 规格编码(稳定): color / capacity
         */
        private String specCode;
        /**
         * 规格名称(可本地化)
         */
        private String specName;
        /**
         * 规格类型
         * <ul>
         *      <li>{@code COLOR}</li>
         *      <li>{@code SIZE}</li>
         *      <li>{@code CAPACITY}</li>
         *      <li>{@code MATERIAL}</li>
         *      <li>{@code OTHER}</li>
         * </ul>
         */
        private SpecType specType;
        /**
         * 是否必选(每个SKU必须选择一个值)
         */
        private Boolean isRequired;
        /**
         * 规格 I18N
         */
        private SpecI18nRespond i18n;
        /**
         * 规格 I18N 列表
         */
        private List<SpecI18nRespond> i18nList;
        /**
         * 规格值列表
         */
        private List<SpecValueRespond> values;

        /**
         * 从给定的 <code>ProductSpec</code> 和语言环境创建一个 <code>SpecRespond</code> 对象
         *
         * @param spec   ProductSpec 对象, 包含规格信息
         * @param locale 字符串, 指定的语言环境代码, 用于国际化处理
         * @return SpecRespond 根据提供的规格和语言环境构建的对象
         */
        public static SpecRespond from(ProductSpec spec, String locale) {
            List<SpecI18nRespond> i18nList = spec.getI18nList() == null ? List.of()
                    : spec.getI18nList()
                    .stream()
                    .map(item -> new SpecI18nRespond(item.getLocale(), item.getSpecName()))
                    .toList();
            if (i18nList.isEmpty() && spec.getI18nName() != null && locale != null)
                i18nList = List.of(new SpecI18nRespond(locale, spec.getI18nName()));

            SpecI18nRespond i18n = null;
            if (locale != null && !i18nList.isEmpty())
                i18n = i18nList.stream()
                        .filter(item -> locale.equalsIgnoreCase(item.getLocale()))
                        .findFirst()
                        .orElse(i18nList.get(0));
            if (i18n == null && spec.getI18nName() != null && locale != null)
                i18n = new SpecI18nRespond(locale, spec.getI18nName());

            String displayName = i18n != null
                    ? i18n.getSpecName()
                    : spec.getI18nName() != null ? spec.getI18nName() : spec.getSpecName();
            // 从 ProductSpec 中获取规格值列表, 并转换为 本地化后的 SpecValueRespond 列表
            List<SpecValueRespond> specValueList = spec.getValues() == null ? List.of()
                    : spec.getValues()
                    .stream()
                    .map(value -> SpecValueRespond.from(value, locale))
                    .toList();
            return new SpecRespond(
                    spec.getId(),
                    spec.getSpecCode(),
                    displayName,
                    spec.getSpecType(),
                    spec.isRequired(),
                    i18n,
                    i18nList,
                    specValueList
            );
        }
    }

    /**
     * 规格多语言响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecI18nRespond {
        /**
         * I18N 语言环境
         */
        private String locale;
        /**
         * 本地化规格名称
         */
        private String specName;
    }

    /**
     * 规格值响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecValueRespond {
        /**
         * 规格值 ID
         */
        private Long valueId;
        /**
         * 规格值编码(稳定)
         */
        private String valueCode;
        /**
         * 规格值名称(可本地化)
         */
        private String valueName;
        /**
         * 规格值属性(JSON)
         */
        private Object attributes;
        /**
         * 规格值 I18N 响应
         */
        private SpecValueI18nRespond i18n;
        /**
         * 规格值 I18N 响应列表
         */
        private List<SpecValueI18nRespond> i18nList;

        public static SpecValueRespond from(ProductSpecValue value, String locale) {
            List<SpecValueI18nRespond> i18nList = value.getI18nList() == null ? List.of()
                    : value.getI18nList()
                    .stream()
                    .map(item -> new SpecValueI18nRespond(item.getLocale(), item.getValueName()))
                    .toList();
            if (i18nList.isEmpty() && value.getI18nName() != null && locale != null)
                i18nList = List.of(new SpecValueI18nRespond(locale, value.getI18nName()));

            SpecValueI18nRespond i18n = null;
            if (locale != null && !i18nList.isEmpty())
                i18n = i18nList.stream()
                        .filter(item -> locale.equalsIgnoreCase(item.getLocale()))
                        .findFirst()
                        .orElse(i18nList.get(0));
            if (i18n == null && value.getI18nName() != null && locale != null)
                i18n = new SpecValueI18nRespond(locale, value.getI18nName());

            String displayName = i18n != null
                    ? i18n.getValueName()
                    : value.getI18nName() != null ? value.getI18nName() : value.getValueName();
            return new SpecValueRespond(
                    value.getId(),
                    value.getValueCode(),
                    displayName,
                    value.getAttributes(),
                    i18n,
                    i18nList
            );
        }
    }

    /**
     * 规格值多语言
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecValueI18nRespond {
        /**
         * I18N 语言环境
         */
        private String locale;
        /**
         * 本地化规格值名称
         */
        private String valueName;
    }

    /**
     * SKU 响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkuRespond {
        /**
         * SKU ID
         */
        private Long id;
        /**
         * SKU编码(外部/条码等)
         */
        private String skuCode;
        /**
         * SKU库存
         */
        private Integer stock;
        /**
         * 重量(kg)
         */
        private BigDecimal weight;
        /**
         * SKU状态
         *
         * @see SkuStatus
         */
        private SkuStatus status;
        /**
         * 是否默认SKU
         */
        private Boolean isDefault;
        /**
         * 条码(可空)
         */
        private String barcode;
        /**
         * SKU 价格响应对象
         */
        private ProductPriceRespond price;
        /**
         * SKU 关联的规格值响应列表
         */
        private List<ProductSkuSpecRespond> specs;
        /**
         * SKU 的图片列表
         */
        private List<ProductImageRespond> images;

        /**
         * 将 <code>ProductSku</code> 实体转换为 <code>SkuRespond</code> 响应对象
         *
         * @param sku 待转换的 <code>ProductSku</code> 实体
         * @return 转换后的 <code>SkuRespond</code> 对象，包含了 SKU 的 ID, 编码, 库存, 重量, 状态, 是否默认, 条码, 价格响应, 规格响应列表, 图片响应列表等信息
         */
        public static SkuRespond from(ProductSku sku) {
            // 从 ProductSku 中获取价格信息, 不为空则构建为 ProductPriceRespond
            ProductPrice price = sku.getPrice();
            ProductPriceRespond priceRespond = price == null ? null : new ProductPriceRespond(
                    price.getCurrency(),
                    price.getListPrice(),
                    price.getSalePrice(),
                    price.isActive()
            );
            // 从 ProductSku 中获取关联的规格值列表, 并转换为 ProductSkuSpecRespond 列表
            List<ProductSkuSpecRespond> specs = sku.getSpecs() == null ? List.of()
                    : sku.getSpecs()
                    .stream()
                    .map(ProductSkuSpecRespond::from)
                    .toList();
            // 从 ProductSku 中获取图片列表, 并转换为 ProductImageRespond 列表
            List<ProductImageRespond> images = sku.getImages() == null ? List.of()
                    : sku.getImages()
                    .stream()
                    .map(ProductImageRespond::from)
                    .toList();
            return new SkuRespond(
                    sku.getId(),
                    sku.getSkuCode(),
                    sku.getStock(),
                    sku.getWeight(),
                    sku.getStatus(),
                    sku.isDefault(),
                    sku.getBarcode(),
                    priceRespond,
                    specs,
                    images
            );
        }
    }

    /**
     * SKU 价格响应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductPriceRespond {
        /**
         * 价格结算货币
         */
        private String currency;
        /**
         * 商品原价
         */
        private BigDecimal listPrice;
        /**
         * 商品折扣价
         */
        private BigDecimal salePrice;
        /**
         * 是否有效
         */
        private Boolean isActive;
    }

    /**
     * SKU 关联的规格值 响应对象
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSkuSpecRespond {
        /**
         * 规格 ID
         */
        private Long specId;
        /**
         * 规格编码(稳定)
         */
        private String specCode;
        /**
         * 规格名称
         */
        private String specName;
        /**
         * 规格值 ID
         */
        private Long valueId;
        /**
         * 规格值编码(稳定)
         */
        private String valueCode;
        /**
         * 规格值名称
         */
        private String valueName;

        /**
         * 从 <code>ProductSkuSpec</code> 对象转换为 <code>ProductSkuSpecRespond</code> 对象
         *
         * @param spec 需要转换的 <code>ProductSkuSpec</code> 对象, 包含规格 ID, 规格编码, 规格名称, 规格值 ID, 规格值编码, 和 规格值名称
         * @return 转换后的 <code>ProductSkuSpecRespond</code> 对象
         */
        public static ProductSkuSpecRespond from(ProductSkuSpec spec) {
            return new ProductSkuSpecRespond(spec.getSpecId(), spec.getSpecCode(), spec.getSpecName(),
                    spec.getValueId(), spec.getValueCode(), spec.getValueName());
        }
    }

    /**
     * 商品 i18n
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductI18nRespond {
        /**
         * 本地化语言环境
         */
        private String locale;
        /**
         * 本地化标题
         */
        private String title;
        /**
         * 本地化副标题
         */
        private String subtitle;
        /**
         * 本地化描述
         */
        private String description;
        /**
         * 本地化别名(SEO/路由)
         */
        private String slug;
        /**
         * 本地化标签(JSON)
         */
        private List<String> tags;

        /**
         * 从 <code>ProductI18n</code> 对象构建一个新的 <code>ProductI18nRespond</code> 实例。
         *
         * @param vo 用于转换的 <code>ProductI18n</code> 对象
         * @return 基于给定 <code>ProductI18n</code> 构建的新 <code>ProductI18nRespond</code> 实例
         */
        public static ProductI18nRespond from(ProductI18n vo) {
            return new ProductI18nRespond(vo.getLocale(), vo.getTitle(), vo.getSubtitle(),
                    vo.getDescription(), vo.getSlug(), vo.getTags());
        }
    }
}
