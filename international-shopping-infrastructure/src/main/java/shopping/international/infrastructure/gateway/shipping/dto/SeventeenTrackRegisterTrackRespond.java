package shopping.international.infrastructure.gateway.shipping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 17Track 注册运单响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeventeenTrackRegisterTrackRespond {

    /**
     * 业务码, 0 通常表示成功
     */
    @Nullable
    private Integer code;
    /**
     * 响应数据
     */
    @Nullable
    private DataPayload data;

    /**
     * 响应数据载体
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPayload {
        /**
         * 接受列表
         */
        @Nullable
        private List<AcceptedItem> accepted;
        /**
         * 拒绝列表
         */
        @Nullable
        private List<RejectedItem> rejected;
    }

    /**
     * 接受注册的运单元素
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AcceptedItem {
        /**
         * 运输商识别方式
         * <ul>
         *     <li>{@code 1:} 准确识别 (包含强制矫正过的)</li>
         *     <li>{@code 2:} 用户输入</li>
         *     <li>{@code 3:} 自动检测 (不确保准确)</li>
         * </ul>
         */
        private Integer origin;
        /**
         * 物流单号
         */
        private String number;
        /**
         * 运输商代码
         */
        private Integer carrier;
        /**
         * 	邮箱
         */
        private String email;
        /**
         * 语言
         */
        private String lang;
    }

    /**
     * 拒绝注册的运单元素
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectedItem {
        /**
         * 物流单号
         */
        private String number;
        /**
         * 运输商代码
         */
        private Integer carrier;
        /**
         * 错误信息
         */
        private ErrorPayload error;
    }

    /**
     * 错误信息载体
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorPayload {
        /**
         * 错误代码
         */
        private Integer code;
        /**
         * 错误信息
         */
        private String message;
    }
}
