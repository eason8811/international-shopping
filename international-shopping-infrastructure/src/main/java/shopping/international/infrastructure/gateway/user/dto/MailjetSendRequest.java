package shopping.international.infrastructure.gateway.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * Mailjet /send 请求体的最外层结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailjetSendRequest {

    /**
     * 消息列表, Mailjet 约定字段名为 "Messages" (注意大小写)
     */
    @JsonProperty("Messages")
    private List<MailjetMessage> messages;

    /**
     * 单封邮件内容
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MailjetMessage {

        /**
         * 发件人
         */
        @JsonProperty("From")
        private MailjetEmail from;

        /**
         * 收件人列表 (至少一个)
         */
        @JsonProperty("To")
        private List<MailjetEmail> to;

        /**
         * 主题
         */
        @JsonProperty("Subject")
        private String subject;

        /**
         * 纯文本内容
         */
        @JsonProperty("TextPart")
        private String textPart;

        /**
         * HTML 内容
         */
        @JsonProperty("HTMLPart")
        private String htmlPart;

        /**
         * 自定义 ID (可选)
         */
        @JsonProperty("CustomID")
        private String customId;
    }

    /**
     * 邮件地址 + 展示名
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MailjetEmail {

        /**
         * 邮箱地址
         */
        @JsonProperty("Email")
        private String email;

        /**
         * 展示名 (可选)
         */
        @JsonProperty("Name")
        private String name;
    }
}
