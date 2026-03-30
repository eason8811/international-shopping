package shopping.international.infrastructure.gateway.payment.dto;

import lombok.*;

/**
 * PayPal payment_source
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayPalPaymentSource {
    /**
     * paypal
     */
    private Paypal paypal;

    /**
     * payment_source.paypal
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Paypal {
        /**
         * 买家邮箱
         */
        private String emailAddress;
        /**
         * PayPal 账户 ID
         */
        private String accountId;
        /**
         * 账户状态
         */
        private String accountStatus;
        /**
         * 买家姓名
         */
        private Name name;
        /**
         * 买家电话
         */
        private PhoneNumber phoneNumber;
        /**
         * 买家地址
         */
        private Address address;
    }

    /**
     * 姓名
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Name {
        /**
         * 名
         */
        private String givenName;
        /**
         * 姓
         */
        private String surname;
    }

    /**
     * 电话
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhoneNumber {
        /**
         * 国家码
         */
        private String countryCode;
        /**
         * 号码
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
    public static class Address {
        private String addressLine1;
        private String addressLine2;
        private String adminArea2;
        private String adminArea1;
        private String postalCode;
        private String countryCode;
    }
}
