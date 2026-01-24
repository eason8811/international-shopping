package shopping.international.api.req.payment;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import shopping.international.types.utils.Verifiable;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * PayPal Webhook 事件请求体
 *
 * <p>该对象保持 "宽松结构" , 以便兼容 PayPal 侧不同的 {@code event_type} 与资源结构</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayPalWebhookEventRequest implements Verifiable {

    /**
     * 事件 ID (PayPal 侧全局唯一) 
     */
    private String id;

    /**
     * 事件创建时间 (PayPal 侧) 
     */
    @JsonProperty("create_time")
    private OffsetDateTime createTime;

    /**
     * 事件版本
     */
    @JsonProperty("event_version")
    private String eventVersion;

    /**
     * 事件类型 (如 {@code PAYMENT.CAPTURE.COMPLETED} 等) 
     */
    @JsonProperty("event_type")
    private String eventType;

    /**
     * 简要描述
     */
    private String summary;

    /**
     * 资源版本
     */
    @JsonProperty("resource_version")
    private String resourceVersion;

    /**
     * 事件资源体 (结构随 event_type 变化, 保持为 Map) 
     */
    private Map<String, Object> resource;

    /**
     * 相关链接列表
     */
    private List<Link> links;

    /**
     * 兼容 PayPal 侧可能新增的字段
     */
    private final Map<String, Object> extra = new LinkedHashMap<>();

    /**
     * 接收未声明的 JSON 字段
     *
     * @param key   字段名
     * @param value 字段值
     */
    @JsonAnySetter
    public void putExtra(@NotNull String key, Object value) {
        extra.put(key, value);
    }

    /**
     * 输出额外字段 (用于 JSON 序列化) 
     *
     * @return 额外字段映射
     */
    @JsonAnyGetter
    public Map<String, Object> extra() {
        return extra;
    }

    /**
     * 基本参数校验
     */
    @Override
    public void validate() {
        requireNotBlank(id, "id 不能为空");
        requireNotNull(createTime, "createTime 不能为空");
        requireNotBlank(eventVersion, "eventVersion 不能为空");
        requireNotBlank(eventType, "eventType 不能为空");
        requireNotBlank(summary, "summary 不能为空");
        requireNotBlank(resourceVersion, "resourceVersion 不能为空");
        requireNotNull(resource, "resource 不能为空");
        requireNotNull(links, "links 不能为空");
    }

    /**
     * Webhook 链接对象
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Link implements Verifiable {
        /**
         * 链接地址
         */
        private String href;
        /**
         * 链接关系 (rel) 
         */
        private String rel;
        /**
         * HTTP 方法
         */
        private String method;

        /**
         * 基本参数校验
         */
        @Override
        public void validate() {
            requireNotBlank(href, "links.href 不能为空");
            requireNotBlank(rel, "links.rel 不能为空");
            requireNotBlank(method, "links.method 不能为空");
        }
    }
}

