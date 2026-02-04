package shopping.international.infrastructure.gateway.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.currency.CurrencyConfig;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.exceptions.PayPalException;

import java.util.ArrayList;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.normalizeCurrency;
import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * PayPal 创建 Order 请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayPalCreateOrderRequest {
    /**
     * intent (通常为 CAPTURE)
     */
    private String intent;
    /**
     * payment_source
     */
    private PaymentSource paymentSource;
    /**
     * purchase_units
     */
    private List<PurchaseUnit> purchaseUnits;

    /**
     * 向 PayPal 创建订单请求体中填入 Amount 信息
     *
     * @param currency         货币代码
     * @param config           currency 配置
     * @param itemTotal        商品总金额
     * @param shipping         运费
     * @param handling         处理费
     * @param taxTotal         税费总额
     * @param shippingDiscount 运费折扣
     * @param discount         商品折扣
     */
    public void fillAmount(@NotNull String currency,
                           @NotNull CurrencyConfig config, @NotNull Long totalAmount,
                           @Nullable Long itemTotal, @Nullable Long shipping, @Nullable Long handling,
                           @Nullable Long taxTotal, @Nullable Long shippingDiscount, @Nullable Long discount) {
        currency = normalizeCurrency(currency);
        if (!config.code().equals(currency))
            throw new PayPalException("currency 错误, 与 CurrencyConfig 的货币代码不相同");
        if (itemTotal == null)
            itemTotal = 0L;
        if (shipping == null)
            shipping = 0L;
        if (handling == null)
            handling = 0L;
        if (taxTotal == null)
            taxTotal = 0L;
        if (shippingDiscount == null)
            shippingDiscount = 0L;
        if (discount == null)
            discount = 0L;
        if (totalAmount != itemTotal + shipping + handling + taxTotal - shippingDiscount - discount)
            throw new PayPalException("总价必须与各部分的和相等");

        String totalAmountMajor = config.toMajor(totalAmount).toPlainString();
        String itemTotalMajor = itemTotal == 0L ? null : config.toMajor(itemTotal).toPlainString();
        String shippingMajor = shipping == 0L ? null : config.toMajor(shipping).toPlainString();
        String handlingMajor = handling == 0L ? null : config.toMajor(handling).toPlainString();
        String taxTotalMajor = taxTotal == 0L ? null : config.toMajor(taxTotal).toPlainString();
        String shippingDiscountMajor = shippingDiscount == 0L ? null : config.toMajor(shippingDiscount).toPlainString();
        String discountMajor = discount == 0L ? null : config.toMajor(discount).toPlainString();

        PayPalAmount.Breakdown breakdown = PayPalAmount.Breakdown.builder()
                .itemTotal(itemTotalMajor != null ? new PayPalAmount.BreakdownItem(currency, itemTotalMajor) : null)
                .shipping(shippingMajor != null ? new PayPalAmount.BreakdownItem(currency, shippingMajor) : null)
                .handling(handlingMajor != null ? new PayPalAmount.BreakdownItem(currency, handlingMajor) : null)
                .taxTotal(taxTotalMajor != null ? new PayPalAmount.BreakdownItem(currency, taxTotalMajor) : null)
                .shippingDiscount(taxTotalMajor != null ? new PayPalAmount.BreakdownItem(currency, shippingDiscountMajor) : null)
                .discount(shippingDiscountMajor != null ? new PayPalAmount.BreakdownItem(currency, discountMajor) : null)
                .build();

        if (purchaseUnits == null)
            purchaseUnits = new ArrayList<>();
        PayPalAmount amount = new PayPalAmount(currency, totalAmountMajor, breakdown);
        if (purchaseUnits.isEmpty()) {
            purchaseUnits.add(PurchaseUnit.builder().amount(amount).build());
            return;
        }
        purchaseUnits.get(0).amount = amount;
    }

    /**
     * 向 PayPal 创建订单请求体中填入收货地址信息
     *
     * @param fullName            收件人全名, 不能为空
     * @param emailAddress        邮件地址, 不能为空
     * @param phoneCountryCode    电话国家代码, 不能为空
     * @param phoneNationalNumber 国内电话号码, 不能为空
     * @param addressLine1        地址行 1, 不能为空
     * @param addressLine2        地址行 2, 可为空
     * @param adminArea2          行政区 2 (如: 区), 可为空
     * @param adminArea1          行政区 1 (如: 市), 不能为空
     * @param postalCode          邮政编码, 不能为空
     * @param countryCode         国家代码, 不能为空
     * @throws IllegalParamException 如果任一必填参数为空或仅包含空白字符
     */
    public void fillShipping(@NotNull String fullName, @NotNull String emailAddress, @NotNull String phoneCountryCode,
                             @NotNull String phoneNationalNumber, @NotNull String addressLine1,
                             @Nullable String addressLine2, @Nullable String adminArea2, @NotNull String adminArea1,
                             @NotNull String postalCode, @NotNull String countryCode) {
        requireNotBlank(fullName, "fullName 不能为空");
        requireNotBlank(emailAddress, "emailAddress 不能为空");
        requireNotBlank(phoneCountryCode, "phoneCountryCode 不能为空");
        requireNotBlank(phoneNationalNumber, "phoneNationalNumber 不能为空");
        requireNotBlank(addressLine1, "addressLine1 不能为空");
        if (addressLine2 != null)
            requireNotBlank(addressLine2, "addressLine2 不能为空");
        if (adminArea2 != null)
            requireNotBlank(adminArea2, "adminArea2 不能为空");
        requireNotBlank(adminArea1, "adminArea1 不能为空");
        requireNotBlank(postalCode, "postalCode 不能为空");
        requireNotBlank(countryCode, "countryCode 不能为空");

        if (purchaseUnits == null)
            purchaseUnits = new ArrayList<>();
        Shipping shipping = new Shipping(
                Name.builder().fullName(fullName).build(),
                emailAddress,
                PhoneNumber.builder().countryCode(phoneCountryCode).nationalNumber(phoneNationalNumber).build(),
                Address.builder()
                        .addressLine1(addressLine1)
                        .addressLine2(addressLine2)
                        .adminArea2(adminArea2)
                        .adminArea1(adminArea1)
                        .postalCode(postalCode)
                        .countryCode(countryCode)
                        .build()
        );
        if (purchaseUnits.isEmpty()) {
            purchaseUnits.add(PurchaseUnit.builder().shipping(shipping).build());
            return;
        }
        purchaseUnits.get(0).shipping = shipping;
    }

    /**
     * 向 PayPal 创建订单请求体中填入体验上下文信息
     *
     * @param returnUrl          成功回跳 URL, 不能为空
     * @param cancelUrl          取消回跳 URL, 不能为空
     * @param brandName          品牌名称, 不能为空
     * @param locale             locale, 不能为空
     * @param shippingPreference PayPal 侧如何处理收货地址, 可为空, 如果为空则默认设置为 "SET_PROVIDED_ADDRESS"
     * @param userAction         用户行为, 不能为空
     * @throws IllegalParamException 如果任一必填参数为空或仅包含空白字符
     */
    public void fillExperienceContext(@NotNull String returnUrl, @NotNull String cancelUrl, @NotNull String brandName,
                                      @NotNull String locale, @Nullable String shippingPreference, @NotNull String userAction) {
        requireNotBlank(returnUrl, "returnUrl 不能为空");
        requireNotBlank(cancelUrl, "cancelUrl 不能为空");
        requireNotBlank(brandName, "brandName 不能为空");
        requireNotBlank(locale, "locale 不能为空");
        if (shippingPreference != null)
            requireNotBlank(shippingPreference, "shippingPreference 不能为空");
        else
            shippingPreference = "SET_PROVIDED_ADDRESS";
        requireNotBlank(userAction, "userAction 不能为空");
        if (paymentSource == null)
            paymentSource = new PaymentSource();
        if (paymentSource.paypal == null)
            paymentSource.paypal = new Paypal();
        paymentSource.paypal.experienceContext = ExperienceContext.builder()
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .brandName(brandName)
                .locale(locale)
                .shippingPreference(shippingPreference)
                .userAction(userAction)
                .build();
    }

    /**
     * purchase_unit
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseUnit {
        /**
         * amount
         */
        private PayPalAmount amount;
        /**
         * shipping
         */
        private Shipping shipping;
    }

    /**
     * payment_source
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSource {
        /**
         * paypal
         */
        private Paypal paypal;
    }

    /**
     * paypal
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Paypal {
        /**
         * experience_context
         */
        private ExperienceContext experienceContext;
    }

    /**
     * experience_context
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExperienceContext {
        /**
         * 成功回跳 URL
         */
        private String returnUrl;
        /**
         * 取消回跳 URL
         */
        private String cancelUrl;
        /**
         * 品牌名称
         */
        private String brandName;
        /**
         * locale
         */
        private String locale;
        /**
         * PayPal 侧如何处理收货地址
         * <ul>
         *     <li>{@code GET_FROM_FILE:} PayPal 直接使用买家 PayPal 账户里的收货地址</li>
         *     <li>{@code SET_FROM_PROVIDER:} 在创建订单时把地址放到 {@code purchase_units[].shipping} 里传给 PayPal, 买家在 PayPal 侧不能编辑该地址</li>
         *     <li>{@code NO_SHIPPING:} 数字商品/礼品卡等不需要地址</li>
         * </ul>
         */
        private String shippingPreference;
        /**
         * 用户行为
         * <ul>
         *     <li>{@code CONTINUE:} 买家在 PayPal 侧点继续后回到你网站, 再做最终确认/下单完成等动作</li>
         *     <li>{@code PAY_NOW:} 买家在 PayPal 审核页直接完成支付 <b>(确定最终应付金额且不会再改)</b></li>
         * </ul>
         */
        private String userAction;
    }

    /**
     * shipping
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Shipping {
        /**
         * name
         */
        private Name name;
        /**
         * 邮件地址
         */
        private String emailAddress;
        /**
         * 电话号码
         */
        private PhoneNumber phoneNumber;
        /**
         * 地址
         */
        private Address address;
    }

    /**
     * name
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Name {
        /**
         * name
         */
        private String fullName;
    }

    /**
     * 电话号码
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhoneNumber {
        /**
         * country_code
         */
        private String countryCode;
        /**
         * national_number
         */
        private String nationalNumber;
    }

    /**
     * 地址
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Address {
        private String addressLine1;
        private String addressLine2;
        private String adminArea2;
        private String adminArea1;
        private String postalCode;
        private String countryCode;
    }
}

