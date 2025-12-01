package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.products.SkuStatus;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * SKU 增量更新请求 ProductSkuPatchRequest
 *
 * <p>用于更新 SKU 基础信息, 价格与规格绑定通过专用接口维护</p>
 */
@Data
public class ProductSkuPatchRequest {
    /**
     * SKU 编码
     */
    @Nullable
    private String skuCode;
    /**
     * 库存
     */
    @Nullable
    private Integer stock;
    /**
     * 重量
     */
    @Nullable
    private BigDecimal weight;
    /**
     * SKU 状态
     */
    @Nullable
    private SkuStatus status;
    /**
     * 是否默认
     */
    @Nullable
    private Boolean isDefault;
    /**
     * 条码
     */
    @Nullable
    private String barcode;
    /**
     * 图片列表
     */
    @Nullable
    private List<ProductImagePayload> images;

    /**
     * 校验并规范化字段
     *
     * @throws IllegalParamException 当字段非法时抛出 IllegalParamException
     */
    public void validate() {
        if (skuCode != null) {
            requireNotBlank(skuCode, "SKU 编码不能为空");
            skuCode = skuCode.strip();
            if (skuCode.length() > 64)
                throw new IllegalParamException("SKU 编码长度不能超过 64 个字符");
        }
        if (stock != null && stock < 0)
            throw new IllegalParamException("SKU 库存不能为负数");
        if (weight != null && weight.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalParamException("SKU 重量不能为负数");
        if (barcode != null) {
            barcode = barcode.strip();
            if (barcode.isEmpty())
                barcode = null;
            else if (barcode.length() > 64)
                throw new IllegalParamException("SKU 条码长度不能超过 64 个字符");
        }
        if (images != null) {
            List<ProductImagePayload> normalized = new ArrayList<>();
            for (ProductImagePayload image : images) {
                if (image == null)
                    continue;
                image.validate();
                normalized.add(image);
            }
            images = normalized;
        }
    }
}
