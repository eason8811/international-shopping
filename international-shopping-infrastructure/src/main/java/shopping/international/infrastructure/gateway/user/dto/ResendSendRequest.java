package shopping.international.infrastructure.gateway.user.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * <p>ResendSendRequest 类用于封装重发邮件请求所需的信息。该类通过 Lombok 的 {@code @Data} 和 {@code @Builder} 注解简化了对象的创建和访问过程。</p>
 *
 * <p>此类包含了发送邮件时需要提供的基本属性, 如发件人, 收件人列表, 邮件主题以及邮件内容(支持 HTML 格式及纯文本格式)。</p>
 */
@Data
@Builder
public class ResendSendRequest {
    /**
     * 发件人邮箱地址, 用于标识发送邮件的来源
     */
    private String from;
    /**
     * 收件人邮箱地址列表, 用于指定邮件接收者
     */
    private List<String> to;
    /**
     * 邮件主题, 用于描述邮件的主要内容或目的
     */
    private String subject;
    /**
     * HTML 格式的邮件内容, 用于以更丰富的格式展示邮件信息。此字段支持使用 HTML 标签来定制邮件的样式和布局
     */
    private String html;
    /**
     * 纯文本格式的邮件内容, 作为 HTML 格式内容的回退选项。当收件人的邮件客户端不支持显示 HTML 内容时, 将使用此字段提供的纯文本内容进行展示
     */
    private String text;
}
