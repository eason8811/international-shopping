package shopping.international.domain.model.vo.products;

import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.products.SkuStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * SKU 基础信息增量更新命令 ProductSkuPatchCommand
 */
@Getter
@ToString
public class ProductSkuPatchCommand {
    /**
     * SKU 编码
     */
    @Nullable
    private final String skuCode;
    /**
     * 库存
     */
    @Nullable
    private final Integer stock;
    /**
     * 重量
     */
    @Nullable
    private final BigDecimal weight;
    /**
     * 状态
     */
    @Nullable
    private final SkuStatus status;
    /**
     * 是否默认
     */
    @Nullable
    private final Boolean isDefault;
    /**
     * 条码
     */
    @Nullable
    private final String barcode;
    /**
     * 图片列表
     */
    @Nullable
    private final List<ProductImage> images;

    private ProductSkuPatchCommand(@Nullable String skuCode,
                                   @Nullable Integer stock,
                                   @Nullable BigDecimal weight,
                                   @Nullable SkuStatus status,
                                   @Nullable Boolean isDefault,
                                   @Nullable String barcode,
                                   @Nullable List<ProductImage> images) {
        this.skuCode = skuCode;
        this.stock = stock;
        this.weight = weight;
        this.status = status;
        this.isDefault = isDefault;
        this.barcode = barcode;
        this.images = images == null ? null : List.copyOf(images);
    }

    /**
     * 构建 SKU 基础信息更新命令
     *
     * @param skuCode  SKU 编码
     * @param stock    库存
     * @param weight   重量
     * @param status   状态
     * @param isDefault 是否默认
     * @param barcode  条码
     * @param images   图片列表
     * @return 更新命令
     */
    public static ProductSkuPatchCommand of(@Nullable String skuCode,
                                            @Nullable Integer stock,
                                            @Nullable BigDecimal weight,
                                            @Nullable SkuStatus status,
                                            @Nullable Boolean isDefault,
                                            @Nullable String barcode,
                                            @Nullable List<ProductImage> images) {
        return new ProductSkuPatchCommand(skuCode, stock, weight, status, isDefault, barcode, images);
    }
}
