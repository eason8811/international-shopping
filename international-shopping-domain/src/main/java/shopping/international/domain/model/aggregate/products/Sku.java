package shopping.international.domain.model.aggregate.products;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import shopping.international.domain.model.enums.products.SkuStatus;
import shopping.international.domain.model.enums.products.StockAdjustMode;
import shopping.international.domain.model.vo.products.ProductImage;
import shopping.international.domain.model.vo.products.ProductPrice;
import shopping.international.domain.model.vo.products.SkuSpecRelation;
import shopping.international.types.utils.Verifiable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * SKU 聚合根, 对应表 product_sku
 *
 * <p>职责: 维护库存、价格、多规格选择、启停状态以及默认选中标识</p>
 */
@Getter
@ToString
@Accessors(chain = true)
public class Sku implements Verifiable {
    /**
     * SKU ID (可空表示未持久化)
     */
    private Long id;
    /**
     * 所属 SPU ID, 不可为空
     */
    private final Long productId;
    /**
     * SKU 编码 (外部/条码)
     */
    private String skuCode;
    /**
     * 当前可售库存
     */
    private int stock;
    /**
     * 重量 (kg)
     */
    private BigDecimal weight;
    /**
     * 状态 (启用/禁用)
     */
    private SkuStatus status;
    /**
     * 是否默认展示 SKU
     */
    private boolean defaultSku;
    /**
     * 条码
     */
    private String barcode;
    /**
     * 多币种价格列表, currency 唯一
     */
    private List<ProductPrice> prices;
    /**
     * 规格选择列表, 每规格仅一条
     */
    private List<SkuSpecRelation> specs;
    /**
     * SKU 图库
     */
    private List<ProductImage> images;
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
     * @param id         SKU ID
     * @param productId  所属商品 ID
     * @param skuCode    SKU 编码
     * @param stock      库存
     * @param weight     重量
     * @param status     状态
     * @param defaultSku 是否默认
     * @param barcode    条码
     * @param prices     价格列表
     * @param specs      规格选择
     * @param images     图库
     * @param createdAt  创建时间
     * @param updatedAt  更新时间
     */
    private Sku(Long id, Long productId, String skuCode, int stock, BigDecimal weight, SkuStatus status,
                boolean defaultSku, String barcode, List<ProductPrice> prices, List<SkuSpecRelation> specs,
                List<ProductImage> images, LocalDateTime createdAt, LocalDateTime updatedAt) {
        requireNotNull(productId, "商品 ID 不能为空");
        require(stock >= 0, "库存不能为负数");
        this.id = id;
        this.productId = productId;
        this.skuCode = skuCode == null ? null : skuCode.strip();
        this.stock = stock;
        this.weight = weight;
        this.status = status == null ? SkuStatus.ENABLED : status;
        this.defaultSku = defaultSku;
        this.barcode = barcode == null ? null : barcode.strip();
        this.prices = normalizeDistinctList(prices, ProductPrice::validate, ProductPrice::getCurrency, "价格列表 currency 不能重复");
        this.specs = normalizeDistinctList(specs, Verifiable::validate,
                selection -> selection.getSpecCode() != null ? selection.getSpecCode() : selection.getSpecId(),
                "同一规格只能选择一个值");
        this.images = normalizeFieldList(images, ProductImage::validate);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建 SKU
     *
     * @param productId  所属商品 ID
     * @param skuCode    SKU 编码
     * @param stock      库存
     * @param weight     重量
     * @param status     状态
     * @param defaultSku 是否默认
     * @param barcode    条码
     * @param prices     价格列表
     * @param specs      规格选择
     * @param images     图库
     * @return 新建的 SKU 聚合根
     */
    public static Sku create(Long productId, String skuCode, int stock, BigDecimal weight, SkuStatus status,
                             boolean defaultSku, String barcode, List<ProductPrice> prices,
                             List<SkuSpecRelation> specs, List<ProductImage> images) {
        if (skuCode != null)
            requireNotBlank(skuCode, "SKU 编码不能为空");
        return new Sku(null, productId, skuCode, stock, weight, status, defaultSku, barcode, prices, specs, images,
                LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * 重建 SKU
     *
     * @param id         SKU ID
     * @param productId  所属商品 ID
     * @param skuCode    SKU 编码
     * @param stock      库存
     * @param weight     重量
     * @param status     状态
     * @param defaultSku 是否默认
     * @param barcode    条码
     * @param prices     价格列表
     * @param specs      规格选择
     * @param images     图库
     * @param createdAt  创建时间
     * @param updatedAt  更新时间
     * @return 重建的 SKU
     */
    public static Sku reconstitute(Long id, Long productId, String skuCode, int stock, BigDecimal weight, SkuStatus status,
                                   boolean defaultSku, String barcode, List<ProductPrice> prices,
                                   List<SkuSpecRelation> specs, List<ProductImage> images,
                                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Sku(id, productId, skuCode, stock, weight, status, defaultSku, barcode, prices, specs, images, createdAt, updatedAt);
    }

    /**
     * 更新基础字段
     *
     * @param skuCode    新编码, 为空则忽略
     * @param weight     新重量, 为空则忽略
     * @param status     新状态, 为空则忽略
     * @param defaultSku 默认标记, 为空则忽略
     * @param barcode    条码, 为空则忽略
     */
    public void updateBasic(String skuCode, BigDecimal weight, SkuStatus status, Boolean defaultSku, String barcode) {
        if (skuCode != null) {
            requireNotBlank(skuCode, "SKU 编码不能为空");
            this.skuCode = skuCode.strip();
        }
        if (weight != null)
            this.weight = weight;
        if (status != null)
            this.status = status;
        if (defaultSku != null)
            this.defaultSku = defaultSku;
        if (barcode != null)
            this.barcode = barcode.strip();
    }

    /**
     * 调整库存
     *
     * @param mode     调整模式
     * @param quantity 数量, SET 允许 0, 其余模式要求大于 0
     */
    public void adjustStock(StockAdjustMode mode, int quantity) {
        requireNotNull(mode, "库存调整模式不能为空");
        if (mode == StockAdjustMode.SET) {
            require(quantity >= 0, "库存不能为负数");
            this.stock = quantity;
        } else if (mode == StockAdjustMode.INCREASE) {
            require(quantity > 0, "增加库存数量必须大于 0");
            this.stock = Math.addExact(this.stock, quantity);
        } else if (mode == StockAdjustMode.DECREASE) {
            require(quantity > 0, "减少库存数量必须大于 0");
            int newStock = this.stock - quantity;
            require(newStock >= 0, "库存不足");
            this.stock = newStock;
        }
    }

    /**
     * 新增价格 (currency 不可重复)
     *
     * @param price 新价格
     */
    public void addPrice(ProductPrice price) {
        requireNotNull(price, "价格不能为空");
        price.validate();
        List<ProductPrice> mutable = prices == null ? new ArrayList<>() : new ArrayList<>(prices);
        boolean exists = mutable.stream().anyMatch(p -> p.getCurrency().equals(price.getCurrency()));
        require(!exists, "价格 currency 已存在: " + price.getCurrency());
        mutable.add(price);
        this.prices = normalizeDistinctList(mutable, ProductPrice::validate, ProductPrice::getCurrency, "价格列表 currency 不能重复");
    }

    /**
     * 更新已有价格 (按 currency 定位, 为空字段不更新)
     *
     * @param currency  货币代码, 必填
     * @param listPrice 标价, null 则保留
     * @param salePrice 促销价, null 则保留
     * @param active    是否启用, null 则保留
     */
    public void updatePrice(String currency, BigDecimal listPrice, BigDecimal salePrice, Boolean active) {
        String normalizedCurrency = normalizeCurrency(currency);
        requireNotNull(normalizedCurrency, "价格 currency 不能为空");
        List<ProductPrice> mutable = prices == null ? new ArrayList<>() : new ArrayList<>(prices);
        ProductPrice existing = mutable.stream()
                .filter(p -> p.getCurrency().equals(normalizedCurrency))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("价格不存在: " + normalizedCurrency));
        BigDecimal targetList = listPrice != null ? listPrice : existing.getListPrice();
        requireNotNull(targetList, "标价不能为空");
        require(targetList.compareTo(BigDecimal.ZERO) > 0, "标价必须大于 0");
        BigDecimal targetSale = salePrice != null ? salePrice : existing.getSalePrice();
        if (targetSale != null) {
            require(targetSale.compareTo(BigDecimal.ZERO) > 0, "促销价必须大于 0");
            require(targetSale.compareTo(targetList) <= 0, "促销价不能高于标价");
        }
        boolean targetActive = active != null ? active : existing.isActive();
        ProductPrice patched = ProductPrice.of(normalizedCurrency, targetList, targetSale, targetActive);
        mutable.removeIf(p -> p.getCurrency().equals(normalizedCurrency));
        mutable.add(patched);
        this.prices = normalizeDistinctList(mutable, ProductPrice::validate, ProductPrice::getCurrency, "价格列表 currency 不能重复");
    }

    /**
     * 替换规格选择
     *
     * @param specs 新规格选择
     */
    public void replaceSpecs(List<SkuSpecRelation> specs) {
        this.specs = normalizeDistinctList(specs, Verifiable::validate,
                selection -> selection.getSpecCode() != null ? selection.getSpecCode() : selection.getSpecId(),
                "同一规格只能选择一个值");
    }

    /**
     * 替换 SKU 图库
     *
     * @param images 新图库
     */
    public void replaceImages(List<ProductImage> images) {
        this.images = normalizeFieldList(images, ProductImage::validate);
    }

    /**
     * 标记为默认 SKU
     */
    public void markDefault() {
        this.defaultSku = true;
    }

    /**
     * 取消默认标记
     */
    public void unmarkDefault() {
        this.defaultSku = false;
    }

    /**
     * 新增规格绑定 (按规格键去重)
     *
     * @param relation 规格选择
     */
    public void addSpecSelection(SkuSpecRelation relation) {
        requireNotNull(relation, "规格选择不能为空");
        relation.validate();
        List<SkuSpecRelation> mutable = this.specs == null ? new ArrayList<>() : new ArrayList<>(this.specs);
        Object specKey = relation.getSpecCode() != null ? relation.getSpecCode() : relation.getSpecId();
        boolean exists = mutable.stream().anyMatch(item -> {
            Object key = item.getSpecCode() != null ? item.getSpecCode() : item.getSpecId();
            return Objects.equals(key, specKey);
        });
        require(!exists, "SKU 已存在该规格绑定: " + specKey);
        mutable.add(relation);
        this.specs = normalizeDistinctList(mutable, Verifiable::validate,
                sel -> sel.getSpecCode() != null ? sel.getSpecCode() : sel.getSpecId(),
                "同一规格只能选择一个值");
    }

    /**
     * 更新已有规格绑定 (按规格键定位, 完整覆盖该规格的取值)
     *
     * @param relation 新的规格选择
     */
    public void updateSpecSelection(SkuSpecRelation relation) {
        requireNotNull(relation, "规格选择不能为空");
        relation.validate();
        List<SkuSpecRelation> mutable = this.specs == null ? new ArrayList<>() : new ArrayList<>(this.specs);
        Object specKey = relation.getSpecCode() != null ? relation.getSpecCode() : relation.getSpecId();
        boolean removed = mutable.removeIf(item -> {
            Object key = item.getSpecCode() != null ? item.getSpecCode() : item.getSpecId();
            return Objects.equals(key, specKey);
        });
        require(removed, "SKU 未绑定该规格: " + specKey);
        mutable.add(relation);
        this.specs = normalizeDistinctList(mutable, Verifiable::validate,
                sel -> sel.getSpecCode() != null ? sel.getSpecCode() : sel.getSpecId(),
                "同一规格只能选择一个值");
    }

    /**
     * 为 SKU 分配 ID (幂等)
     *
     * @param id 新 ID
     */
    public void assignId(Long id) {
        requireNotNull(id, "SKU ID 不能为空");
        if (this.id != null && !Objects.equals(this.id, id))
            throw new IllegalStateException("SKU 已存在 ID, 不允许覆盖, current=" + this.id + ", new=" + id);
        this.id = id;
    }

    /**
     * 校验 SKU 聚合根
     */
    @Override
    public void validate() {
        requireNotNull(productId, "商品 ID 不能为空");
        requireNotNull(status, "SKU 状态不能为空");
        require(stock >= 0, "库存不能为负数");
    }
}
