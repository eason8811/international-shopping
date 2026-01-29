package shopping.international.domain.model.vo.payment;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.payment.PaymentChannel;
import shopping.international.domain.model.enums.payment.PaymentStatus;

import java.time.LocalDateTime;

/**
 * PayPal Checkout 事务内准备结果
 *
 * @param orderId             订单 ID
 * @param orderNo             订单编号
 * @param currency            使用的货币代码 (ISO 4217)
 * @param totalAmount         总金额, 单位为最小货币单位
 * @param itemTotal           商品总金额, 可为空
 * @param shipping            运费, 可为空
 * @param handling            处理费用, 可为空
 * @param taxTotal            税费总额, 可为空
 * @param shippingDiscount    运费折扣, 可为空
 * @param discount            折扣总额, 可为空
 * @param fullName            买家全名
 * @param emailAddress        买家邮箱地址
 * @param phoneCountryCode    买家电话国家代码
 * @param phoneNationalNumber 买家国内电话号码
 * @param addressLine1        地址行 1
 * @param addressLine2        地址行 2, 可为空
 * @param adminArea2          次级行政区划名称, 如县/区, 可为空
 * @param adminArea1          主要行政区划名称, 如省/市
 * @param postalCode          邮政编码
 * @param countryCode         国家代码
 * @param channel             支付渠道, 枚举类型 {@link PaymentChannel}
 * @param paymentStatus       支付状态, 枚举类型 {@link PaymentStatus}
 * @param orderCreatedAt      订单创建时间
 * @param paymentId           支付单 ID
 * @param paypalOrderId       PayPal 订单 ID, 可为空
 */
@Builder
public record PayPalCheckoutResult(
        @NotNull Long orderId,
        @NotNull String orderNo,
        @NotNull String currency,
        @NotNull Long totalAmount,
        @Nullable Long itemTotal,
        @Nullable Long shipping,
        @Nullable Long handling,
        @Nullable Long taxTotal,
        @Nullable Long shippingDiscount,
        @Nullable Long discount,
        @NotNull String fullName,
        @NotNull String emailAddress,
        @NotNull String phoneCountryCode,
        @NotNull String phoneNationalNumber,
        @NotNull String addressLine1,
        @Nullable String addressLine2,
        @Nullable String adminArea2,
        @NotNull String adminArea1,
        @NotNull String postalCode,
        @NotNull String countryCode,
        @NotNull PaymentChannel channel,
        @NotNull PaymentStatus paymentStatus,
        @NotNull LocalDateTime orderCreatedAt,
        @NotNull Long paymentId,
        @Nullable String paypalOrderId) {
}
