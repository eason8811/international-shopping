package shopping.international.infrastructure.gateway.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Mailjet /send 响应体
 *
 * <p>这里只定义最小必要字段用于判断成功/失败</p>
 */
@Data
public class MailjetSendResponse {

    /**
     * 消息结果列表
     */
    @JsonProperty("Messages")
    private List<ResponseMessage> messages;

    /**
     * 单封消息的发送结果
     */
    @Data
    public static class ResponseMessage {
        /**
         * 结果状态: success 或 error
         */
        @JsonProperty("Status")
        private String status;
        /**
         * 收件人维度的结果 (省略字段不影响成功判断)
         */
        @JsonProperty("To")
        private List<ToResult> to;
        /**
         * 错误列表 (当 Status != success 时可能存在)
         */
        @JsonProperty("Errors")
        private List<MailjetError> errors;
    }

    /**
     * 投递到某个收件人的结果 (最小字段集)
     */
    @Data
    public static class ToResult {
        /**
         * 收件人邮箱
         */
        @JsonProperty("Email")
        private String email;
        /**
         * 消息的唯一标识符, 用于追踪和引用特定的消息实例
         */
        @JsonProperty("MessageUUID")
        private String messageUUID;
        /**
         * 消息的 ID, 用于标识和追踪特定的消息实例
         */
        @JsonProperty("MessageID")
        private Long messageId;
        /**
         * 消息的链接, 用于直接访问或引用消息实例
         */
        @JsonProperty("MessageHref")
        private String messageHref;
    }

    /**
     * Mailjet 错误条目 (最小字段集)
     */
    @Data
    public static class MailjetError {
        /**
         * 错误代码
         */
        @JsonProperty("ErrorCode")
        private String errorCode;
        /**
         * 错误消息
         */
        @JsonProperty("ErrorMessage")
        private String errorMessage;
        /**
         * 状态码
         */
        @JsonProperty("StatusCode")
        private Integer statusCode;
    }
}
