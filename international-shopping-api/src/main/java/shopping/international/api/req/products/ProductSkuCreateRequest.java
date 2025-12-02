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

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 单个 SKU 的维护请求 ProductSkuUpsertRequestItem
 */
@Data
public class ProductSkuCreateRequest {
    /**
     * SKU ID, 创建时留空, 更新时必填
     */
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
    private Boolean isDefault;
    /**
     * 条码, 可选
     */
    @Nullable
    private String barcode;
    /**
     * 多币种价格信息
     */
    @Nullable
    private List<ProductPriceUpsertRequest> price;
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
        requireNotNull(id, "SKU ID 不能为空");
        require(id > 0, "SKU ID 必须大于 0");
        skuCode = requireCreateField(skuCode, "SKU 编码不能为空", c -> c.length() <= 64, "SKU 编码长度不能超过 64 个字符");

        requireNotNull(stock, "SKU 库存不能为空");
        require(stock >= 0, "SKU 库存不能为负数");
        if (weight != null)
            require(weight.compareTo(BigDecimal.ZERO) >= 0, "SKU 重量不能为负数");
        requireNotNull(status, "SKU 状态不能为空");
        isDefault = isDefault != null && isDefault;
        barcode = requirePatchField(barcode, "barcode 不能为空", c -> c.length() <= 64, "SKU 条码长度不能超过 64 个字符");

        if (price == null) {
            price = List.of();
            return;
        }
        List<ProductPriceUpsertRequest> normalizedPriceList = new ArrayList<>();
        Set<String> currencies = new LinkedHashSet<>();
        for (ProductPriceUpsertRequest priceItem : price) {
            if (priceItem == null)
                continue;
            priceItem.createValidate();
            String currency = priceItem.getCurrency() == null ? null : priceItem.getCurrency().strip().toUpperCase();
            if (currency != null && !currencies.add(currency))
                throw new IllegalParamException("同一 SKU 的价格币种不可重复");
            normalizedPriceList.add(priceItem);
        }
        price = normalizedPriceList;

        if (specs == null) {
            specs = List.of();
            return;
        }
        List<ProductSkuSpecUpsertRequest> normalizedSkuSpecList = new ArrayList<>();
        Set<String> specCodes = new LinkedHashSet<>();
        for (ProductSkuSpecUpsertRequest spec : specs) {
            if (spec == null)
                continue;
            spec.validate();
            if (!specCodes.add(spec.getSpecCode()))
                throw new IllegalParamException("同一 SKU 的规格编码不可重复");
            normalizedSkuSpecList.add(spec);
        }
        specs = normalizedSkuSpecList;


        if (images == null) {
            images = List.of();
            return;
        }
        List<ProductImagePayload> normalizedImageList = new ArrayList<>();
        for (ProductImagePayload image : images) {
            if (image == null)
                continue;
            image.validate();
            normalizedImageList.add(image);
        }
        images = normalizedImageList;
    }
}
