package international.shopping.api.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import shopping.international.types.enums.ApiCode;

import java.time.LocalDateTime;

/**
 * 统一返回结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Result<T> {

    /**
     * 业务是否成功 (与 HTTP 状态码无关)
     */
    private boolean success;

    /**
     * 业务码
     *
     * @see ApiCode
     */
    private ApiCode code;

    /**
     * 人类可读的简短信息, 为空时默认取 ApiCode 的默认消息
     */
    private String message;

    /**
     * 服务器时间，统一格式 {@code yyyy-MM-dd HH:mm:ss}
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 请求追踪ID (可由网关/拦截器注入)
     */
    private String traceId;

    /**
     * 业务数据体
     */
    private T data;

    /**
     * 附加信息 (如分页)
     */
    private Meta meta;

    // ======= 工厂方法 =======

    /**
     * 创建一个表示成功的结果对象, 使用默认的业务码 {@link ApiCode#OK}, 没有响应信息, 并携带给定的数据
     *
     * @param <T>  业务数据类型
     * @param data 要包含在结果中的业务数据
     * @return 包含指定数据的成功 {@link Result} 对象
     */
    public static <T> Result<T> ok(T data) {
        return base(true, ApiCode.OK, null, data, null);
    }

    /**
     * 创建一个表示成功的响应结果, 使用默认的业务码 {@link ApiCode#OK}, 没有响应信息, 并携带给定的数据和元信息
     *
     * @param <T>  业务数据类型
     * @param data 要包含在结果中的业务数据
     * @param meta 结果中附加的元信息, 包括分页等信息
     * @return 包含指定数据和元信息的成功 {@link Result} 对象
     */
    public static <T> Result<T> ok(T data, Meta meta) {
        return base(true, ApiCode.OK, null, data, meta);
    }

    /**
     * 创建一个表示资源已创建的结果对象, 使用默认的业务码 {@link ApiCode#CREATED}, 没有响应信息, 并携带给定的数据
     *
     * @param <T>  业务数据类型
     * @param data 要包含在结果中的业务数据
     * @return 包含指定数据且状态为 {@link ApiCode#CREATED} 的 {@link Result} 对象
     */
    public static <T> Result<T> created(T data) {
        return base(true, ApiCode.CREATED, null, data, null);
    }

    /**
     * 创建一个表示请求已接受的结果对象, 使用默认的业务码 {@link ApiCode#ACCEPTED} 和给定的消息
     *
     * @param <T>     业务数据类型
     * @param message 自定义响应消息, 如果为空则使用 {@link ApiCode#ACCEPTED} 的默认消息
     * @return 包含指定消息且状态为 {@link ApiCode#ACCEPTED} 的 {@link Result} 对象
     */
    public static <T> Result<T> accepted(String message) {
        return base(true, ApiCode.ACCEPTED, defaultIfBlank(message, ApiCode.ACCEPTED.getDefaultMessage()), null, null);
    }

    /**
     * 创建一个表示重定向的结果对象, 使用默认的业务码 {@link ApiCode#FOUND}, 没有响应信息, 并携带给定的数据
     *
     * @param <T>  业务数据类型
     * @param data 要包含在结果中的业务数据
     * @return 包含指定数据且状态为 {@link ApiCode#FOUND} 的 {@link Result} 对象
     */
    public static <T> Result<T> found(T data) {
        return base(true, ApiCode.FOUND, null, data, null);
    }

    /**
     * 创建一个表示失败的结果对象, 使用给定的业务码和消息.
     *
     * @param <T>     业务数据类型
     * @param code    业务码, 表示具体的错误类型
     * @param message 自定义响应消息, 如果为空则使用 {@link ApiCode} 的默认消息
     * @return 包含指定业务码和消息的失败 {@link Result} 对象
     */
    public static <T> Result<T> error(ApiCode code, String message) {
        return base(false, code, defaultIfBlank(message, code.getDefaultMessage()), null, null);
    }

    /**
     * 创建一个通用的响应结果对象, 可以灵活地指定业务是否成功, 业务码, 响应信息, 数据及附加元信息.
     *
     * @param <T>     业务数据类型
     * @param success 标识业务是否成功的布尔值
     * @param code    用于标识具体业务状态或错误类型的业务码
     * @param message 响应给调用方的信息, 如果为空则使用业务码默认的消息
     * @param data    将被包含在响应中的业务数据
     * @param meta    结果中附加的元信息, 如分页等
     * @return 包含了指定参数的 {@link Result} 对象
     */
    public static <T> Result<T> of(boolean success, ApiCode code, String message, T data, Meta meta) {
        return base(success, code, message, data, meta);
    }

    // ======= 基础构造 =======

    /**
     * 基础构造方法
     *
     * @param success 业务是否成功
     * @param code    业务码
     * @param message 响应信息
     * @param data    业务数据
     * @param meta    附加元信息
     * @param <T>     业务数据类型
     * @return Result 对象
     */
    private static <T> Result<T> base(boolean success, ApiCode code, String message, T data, Meta meta) {
        return Result.<T>builder()
                .success(success)
                .code(code)
                .message(defaultIfBlank(message, code != null ? code.getDefaultMessage() : null))
                .timestamp(LocalDateTime.now())
                .data(data)
                .meta(meta)
                .build();
    }

    /**
     * 如果给定的字符串 <code>str</code> 为 <code>null</code> 或者是空白, 则返回默认值 <code>def</code>, 否则返回 <code>str</code>.
     *
     * @param str          需要检查的字符串
     * @param defaultValue 当 <code>str</code> 为 <code>null</code> 或空白时返回的默认字符串
     * @return 如果 <code>str</code> 为 <code>null</code> 或空白, 返回 <code>def</code>; 否则返回 <code>str</code>
     */
    private static String defaultIfBlank(String str, String defaultValue) {
        return (str == null || str.isBlank()) ? defaultValue : str;
    }

    // ======= 附加元信息 (分页等) =======

    /**
     * 附加元信息, 如分页信息等
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {
        /**
         * 当前页码 (从 1 开始约定)
         */
        private Integer page;
        /**
         * 每页大小
         */
        private Integer size;
        /**
         * 总条目数
         */
        private Long total;
    }
}
