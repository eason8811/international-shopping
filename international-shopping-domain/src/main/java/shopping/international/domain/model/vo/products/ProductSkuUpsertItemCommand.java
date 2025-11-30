package shopping.international.domain.model.vo.products;

import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.products.SkuStatus;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 单个 SKU 的维护命令 ProductSkuUpsertItemCommand
 *
 * <p>包含 SKU 基础信息、规格绑定、价格与图片, 用于创建或更新单个 SKU</p>
 */
@Getter
@ToString
public class ProductSkuUpsertItemCommand {
    /**
     * SKU ID
     */
    @Nullable
    private final Long id;
    /**
     * SKU 编码
     */
    private final String skuCode;
    /**
     * 库存
     */
    private final int stock;
    /**
     * 重量
     */
    @Nullable
    private final BigDecimal weight;
    /**
     * 状态
     */
    private final SkuStatus status;
    /**
     * 是否默认 SKU
     */
    private final boolean isDefault;
    /**
     * 条码
     */
    @Nullable
    private final String barcode;
    /**
     * 价格信息
     */
    @Nullable
    private final ProductPriceUpsertCommand price;
    /**
     * 规格绑定列表
     */
    private final List<ProductSkuSpecUpsertCommand> specs;
    /**
     * 图片列表
     */
    private final List<ProductImage> images;

    private ProductSkuUpsertItemCommand(@Nullable Long id,
                                        String skuCode,
                                        int stock,
                                        @Nullable BigDecimal weight,
                                        SkuStatus status,
                                        boolean isDefault,
                                        @Nullable String barcode,
                                        @Nullable ProductPriceUpsertCommand price,
                                        List<ProductSkuSpecUpsertCommand> specs,
                                        List<ProductImage> images) {
        this.id = id;
        this.skuCode = skuCode;
        this.stock = stock;
        this.weight = weight;
        this.status = status;
        this.isDefault = isDefault;
        this.barcode = barcode;
        this.price = price;
        this.specs = specs;
        this.images = images;
    }

    /**
     * 构建单个 SKU 的维护命令
     *
     * @param id       SKU ID
     * @param skuCode  SKU 编码
     * @param stock    库存
     * @param weight   重量
     * @param status   状态
     * @param isDefault 是否默认 SKU
     * @param barcode  条码
     * @param price    价格信息
     * @param specs    规格绑定列表
     * @param images   图片列表
     * @return SKU 维护命令
     * @throws IllegalParamException 当必填字段缺失时抛出 IllegalParamException
     */
    public static ProductSkuUpsertItemCommand of(@Nullable Long id,
                                                 String skuCode,
                                                 int stock,
                                                 @Nullable BigDecimal weight,
                                                 SkuStatus status,
                                                 boolean isDefault,
                                                 @Nullable String barcode,
                                                 @Nullable ProductPriceUpsertCommand price,
                                                 @Nullable List<ProductSkuSpecUpsertCommand> specs,
                                                 @Nullable List<ProductImage> images) {
        requireNotBlank(skuCode, "SKU 编码不能为空");
        requireNotNull(status, "SKU 状态不能为空");
        List<ProductSkuSpecUpsertCommand> safeSpecs = specs == null ? List.of() : List.copyOf(specs);
        List<ProductImage> safeImages = images == null ? List.of() : List.copyOf(images);
        return new ProductSkuUpsertItemCommand(id, skuCode, stock, weight, status, isDefault, barcode, price, safeSpecs, safeImages);
    }
}
