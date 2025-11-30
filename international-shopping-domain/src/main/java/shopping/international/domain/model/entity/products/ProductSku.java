package shopping.international.domain.model.entity.products;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.enums.products.SkuStatus;
import shopping.international.domain.model.vo.products.ProductImage;
import shopping.international.domain.model.vo.products.ProductPrice;
import shopping.international.domain.model.vo.products.ProductSkuSpec;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * SKU 实体
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
public class ProductSku {
    /**
     * SKU ID
     */
    private Long id;
    /**
     * 商品 ID
     */
    private Long productId;
    /**
     * SKU 编码
     */
    private String skuCode;
    /**
     * 库存
     */
    private int stock;
    /**
     * 重量
     */
    private BigDecimal weight;
    /**
     * 状态
     */
    private SkuStatus status;
    /**
     * 是否默认
     */
    private boolean isDefault;
    /**
     * 条码
     */
    private String barcode;
    /**
     * 价格
     */
    private ProductPrice price;
    /**
     * 规格选项
     */
    private List<ProductSkuSpec> specs = new ArrayList<>();
    /**
     * 图片
     */
    private List<ProductImage> images = new ArrayList<>();

    private ProductSku() {
    }

    /**
     * 重建 SKU
     *
     * @param id        主键
     * @param productId 商品 ID
     * @param skuCode   编码
     * @param stock     库存
     * @param weight    重量
     * @param status    状态
     * @param isDefault 是否默认
     * @param barcode   条码
     * @return SKU 实体
     */
    public static ProductSku reconstitute(Long id,
                                          Long productId,
                                          String skuCode,
                                          Integer stock,
                                          BigDecimal weight,
                                          SkuStatus status,
                                          Boolean isDefault,
                                          String barcode) {
        if (id == null)
            throw new IllegalParamException("SKU ID 不能为空");
        ProductSku sku = new ProductSku();
        sku.id = id;
        sku.productId = productId;
        sku.skuCode = skuCode;
        sku.stock = stock == null ? 0 : stock;
        sku.weight = weight;
        sku.status = status == null ? SkuStatus.DISABLED : status;
        sku.isDefault = Boolean.TRUE.equals(isDefault);
        sku.barcode = barcode;
        return sku;
    }

    /**
     * 重建 SKU
     *
     * @param id        主键
     * @param skuCode   编码
     * @param stock     库存
     * @param weight    重量
     * @param status    状态
     * @param isDefault 是否默认
     * @param barcode   条码
     * @return SKU 实体
     */
    public static ProductSku reconstitute(Long id,
                                          String skuCode,
                                          Integer stock,
                                          BigDecimal weight,
                                          SkuStatus status,
                                          Boolean isDefault,
                                          String barcode) {
        return reconstitute(id, null, skuCode, stock, weight, status, isDefault, barcode);
    }

    /**
     * 仅用于创建虚拟 SKU (如价格缺失时的兼容)
     */
    public static ProductSku simple(Long id, @NotNull String skuCode) {
        return simple(id, null, skuCode);
    }

    /**
     * 仅用于创建虚拟 SKU (如价格缺失时的兼容)
     *
     * @param id        SKU ID
     * @param productId 商品 ID
     * @param skuCode   SKU 编码
     * @return SKU 实体
     */
    public static ProductSku simple(Long id, Long productId, @NotNull String skuCode) {
        requireNotBlank(skuCode, "SKU 编码不能为空");
        ProductSku sku = new ProductSku();
        sku.id = id;
        sku.productId = productId;
        sku.skuCode = skuCode;
        sku.status = SkuStatus.DISABLED;
        return sku;
    }

    /**
     * 设置价格
     *
     * @param price 价格
     */
    public void attachPrice(ProductPrice price) {
        this.price = price;
    }

    /**
     * 设置规格
     *
     * @param specs 规格列表
     */
    public void attachSpecs(List<ProductSkuSpec> specs) {
        if (specs != null)
            this.specs = new ArrayList<>(specs);
    }

    /**
     * 设置图片
     *
     * @param images 图片列表
     */
    public void attachImages(List<ProductImage> images) {
        if (images != null)
            this.images = new ArrayList<>(images);
    }

    /**
     * 是否可售
     *
     * @return true 表示可售
     */
    public boolean isEnabled() {
        return status == SkuStatus.ENABLED;
    }

    /**
     * 获取不可变的规格列表
     *
     * @return 规格列表
     */
    public List<ProductSkuSpec> getSpecs() {
        return Collections.unmodifiableList(specs);
    }

    /**
     * 获取不可变的图片列表
     *
     * @return 图片列表
     */
    public List<ProductImage> getImages() {
        return Collections.unmodifiableList(images);
    }
}
