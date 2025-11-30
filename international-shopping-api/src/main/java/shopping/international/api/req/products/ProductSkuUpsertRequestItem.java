package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.products.SkuStatus;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 单个 SKU 的维护请求 ProductSkuUpsertRequestItem
 */
@Data
public class ProductSkuUpsertRequestItem {
    /**
     * SKU ID, 创建时留空, 更新时必填
     */
    @Nullable
    private Long id;
    /**
     * SKU 编码, 需在系统内唯一
     */
    private String skuCode;
    /**
     * 可售库存
     */
    private Integer stock;
    /**
     * 重量, 单位千克
     */
    @Nullable
    private BigDecimal weight;
    /**
     * SKU 状态
     */
    private SkuStatus status;
    /**
     * 是否默认展示该 SKU
     */
    @Nullable
    private Boolean isDefault;
    /**
     * 条码, 可选
     */
    @Nullable
    private String barcode;
    /**
     * 价格信息
     */
    @Nullable
    private ProductPriceUpsertRequest price;
    /**
     * 规格绑定列表
     */
    @Nullable
    private List<ProductSkuSpecUpsertRequest> specs;
    /**
     * SKU 专属图片列表
     */
    @Nullable
    private List<ProductImagePayload> images;

    /**
     * 校验并规范化 SKU 字段
     *
     * @throws IllegalParamException 当必填字段缺失或格式非法时抛出 IllegalParamException
     */
    public void validate() {
        if (id != null && id <= 0)
            throw new IllegalParamException("SKU ID 非法");

        requireNotBlank(skuCode, "SKU 编码不能为空");
        skuCode = skuCode.strip();
        if (skuCode.length() > 64)
            throw new IllegalParamException("SKU 编码长度不能超过 64 个字符");

        requireNotNull(stock, "SKU 库存不能为空");
        if (stock < 0)
            throw new IllegalParamException("SKU 库存不能为负数");

        if (weight != null && weight.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalParamException("SKU 重量不能为负数");

        if (status == null)
            throw new IllegalParamException("SKU 状态不能为空");
        if (isDefault == null)
            isDefault = false;

        if (barcode != null) {
            barcode = barcode.strip();
            if (barcode.isEmpty())
                barcode = null;
            else if (barcode.length() > 64)
                throw new IllegalParamException("SKU 条码长度不能超过 64 个字符");
        }

        if (price != null)
            price.validate();

        if (specs == null)
            specs = List.of();
        else {
            List<ProductSkuSpecUpsertRequest> normalized = new ArrayList<>();
            Set<String> specCodes = new LinkedHashSet<>();
            for (ProductSkuSpecUpsertRequest spec : specs) {
                if (spec == null)
                    continue;
                spec.validate();
                if (!specCodes.add(spec.getSpecCode()))
                    throw new IllegalParamException("同一 SKU 的规格编码不可重复");
                normalized.add(spec);
            }
            specs = normalized;
        }

        if (images == null)
            images = List.of();
        else {
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
