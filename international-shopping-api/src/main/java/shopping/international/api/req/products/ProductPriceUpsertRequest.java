package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * SKU 价格维护请求 ProductPriceUpsertRequest
 */
@Data
public class ProductPriceUpsertRequest {
    /**
     * 币种代码, 需符合 ISO 4217 格式
     */
    private String currency;
    /**
     * 标价
     */
    @Nullable
    private BigDecimal listPrice;
    /**
     * 促销价
     */
    @Nullable
    private BigDecimal salePrice;
    /**
     * 价格是否生效
     */
    @Nullable
    private Boolean isActive;

    /**
     * 校验并规范化价格字段, 用于创建操作
     *
     * <p>此方法主要用于在创建商品价格时, 对输入的币种和价格进行合法性校验, 并对部分字段做相应的调整。它确保了币种格式符合 ISO 4217 标准, 且所有涉及的价格值均大于零, 同时促销价不能高于标价。</p>
     *
     * @throws IllegalParamException 当币种或价格不符合要求时抛出该异常
     */
    public void createValidate() {
        requireNotBlank(currency, "价格币种不能为空");
        currency = currency.strip().toUpperCase();
        require(Pattern.matches("^[A-Z]{3}$", currency), "价格币种格式不合法, 需要 3 位大写字母");
        requireNotNull(listPrice, "标价不能为空");
        require(listPrice.compareTo(BigDecimal.ZERO) > 0, "标价必须大于 0");
        if (salePrice != null)
            require(salePrice.compareTo(BigDecimal.ZERO) > 0, "促销价必须大于 0");
        if (salePrice != null)
            require(salePrice.compareTo(listPrice) < 0, "促销价不能高于标价");
        isActive = isActive == null || isActive;
    }

    /**
     * 校验并规范化价格字段, 用于更新操作
     *
     * <p>此方法主要用于在更新商品价格时, 对输入的币种和价格进行合法性校验, 并对部分字段做相应的调整。它确保了币种格式符合 ISO 4217 标准, 且所有涉及的价格值均大于零, 同时促销价不能高于标价。</p>
     *
     * @throws IllegalParamException 当币种或价格不符合要求时抛出该异常
     */
    public void updateValidate() {
        requireNotBlank(currency, "价格币种不能为空");
        currency = currency.strip().toUpperCase();
        require(Pattern.matches("^[A-Z]{3}$", currency), "价格币种格式不合法, 需要 3 位大写字母");
        if (listPrice != null)
            require(listPrice.compareTo(BigDecimal.ZERO) > 0, "标价必须大于 0");
        if (salePrice != null)
            require(salePrice.compareTo(BigDecimal.ZERO) > 0, "促销价必须大于 0");
        if (salePrice != null && listPrice != null)
            require(salePrice.compareTo(listPrice) < 0, "促销价不能高于标价");
        isActive = isActive == null || isActive;
    }
}
