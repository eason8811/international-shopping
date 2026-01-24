package shopping.international.api.req.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.enums.payment.PaymentChannel;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 创建 PayPal Checkout 请求体 (用于发起支付尝试并返回收银台跳转链接) 
 *
 * <p>该请求用于用户在前端点击 "去支付" 时, 向系统创建/复用支付单, 并由系统进一步对接 PayPal 创建 Order</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayPalCheckoutCreateRequest implements Verifiable {

    /**
     * 业务订单号 (对应 orders.order_no) 
     */
    private String orderNo;

    /**
     * 支付通道
     *
     * <p>当前接口仅支持 PayPal 通道, 通常应为 {@link PaymentChannel#PAYPAL}</p>
     */
    private PaymentChannel channel;

    /**
     * 支付成功回跳地址 (前端 URL) 
     */
    private String returnUrl;

    /**
     * 用户取消回跳地址 (前端 URL) 
     */
    private String cancelUrl;

    /**
     * 基本参数校验与字段归一化
     */
    @Override
    public void validate() {
        orderNo = normalizeNotNullField(orderNo, "orderNo 不能为空",
                s -> s.length() == 26, "orderNo 必须为 26 位");

        requireNotNull(channel, "channel 不能为空");
        require(channel == PaymentChannel.PAYPAL, "channel 仅支持 PAYPAL");

        returnUrl = normalizeNotNullField(returnUrl, "returnUrl 不能为空",
                s -> s.length() <= 500, "returnUrl 长度不能超过 500 个字符");
        cancelUrl = normalizeNotNullField(cancelUrl, "cancelUrl 不能为空",
                s -> s.length() <= 500, "cancelUrl 长度不能超过 500 个字符");
    }
}

