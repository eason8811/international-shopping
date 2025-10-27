package shopping.international.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import shopping.international.api.resp.Result;
import shopping.international.types.exceptions.AppException;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.enums.ApiCode;

/**
 * 全局异常处理器。
 *
 * <p>职责：
 * <ul>
 *   <li>拦截系统内常见异常并统一返回 {@link Result}</li>
 *   <li>根据异常类型设置合适的 HTTP 状态码与 {@link ApiCode}</li>
 *   <li>使用 {@code Slf4j} 输出统一格式的错误日志，日志中包含错误原因、请求基本信息与可用的 traceId</li>
 * </ul>
 * </p>
 *
 * <p>HTTP 状态与业务码约定：
 * <ul>
 *   <li>{@link IllegalParamException} → 400 Bad Request / {@link ApiCode#BAD_REQUEST}</li>
 *   <li>{@link AppException} → 500 Internal Server Error / {@link ApiCode#INTERNAL_SERVER_ERROR}</li>
 *   <li>{@link RuntimeException}（兜底） → 500 Internal Server Error / {@link ApiCode#INTERNAL_SERVER_ERROR}</li>
 * </ul>
 * </p>
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * traceId 的请求头名称。可根据网关/链路追踪系统调整。
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * 处理参数错误异常：返回 400。
     *
     * @param ex      抛出的 {@link IllegalParamException}
     * @param request 当前请求
     * @return 统一返回结构 {@link Result}，HTTP 400
     */
    @ExceptionHandler(IllegalParamException.class)
    public ResponseEntity<Result<Void>> handleIllegalParam(final IllegalParamException ex,
                                                           final HttpServletRequest request) {
        // 统一格式日志（参数类异常一般不需要打印堆栈，避免噪音）
        log.warn(buildLogFormat(),
                ex.getClass().getSimpleName(),
                ApiCode.BAD_REQUEST,
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                request.getRequestURI(),
                request.getMethod(),
                request.getRemoteAddr(),
                resolveTraceId(request));

        return respond(HttpStatus.BAD_REQUEST, ApiCode.BAD_REQUEST, ex.getMessage(), request);
    }

    /**
     * 处理系统内自定义异常：返回 500。
     *
     * @param ex      抛出的 {@link AppException}
     * @param request 当前请求
     * @return 统一返回结构 {@link Result}，HTTP 500
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<Result<Void>> handleAppException(final AppException ex,
                                                           final HttpServletRequest request) {
        // 系统异常打印堆栈，便于排查
        log.error(buildLogFormat(),
                ex.getClass().getSimpleName(),
                ApiCode.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage(),
                request.getRequestURI(),
                request.getMethod(),
                request.getRemoteAddr(),
                resolveTraceId(request),
                ex);

        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ApiCode.INTERNAL_SERVER_ERROR, ex.getMessage(), request);
    }

    /**
     * 处理未显式捕获的运行时异常（兜底）：返回 500。
     *
     * @param ex      抛出的 {@link RuntimeException}
     * @param request 当前请求
     * @return 统一返回结构 {@link Result}，HTTP 500
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<Void>> handleRuntimeException(final RuntimeException ex,
                                                               final HttpServletRequest request) {
        log.error(buildLogFormat(),
                ex.getClass().getSimpleName(),
                ApiCode.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage(),
                request.getRequestURI(),
                request.getMethod(),
                request.getRemoteAddr(),
                resolveTraceId(request),
                ex);

        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ApiCode.INTERNAL_SERVER_ERROR, ex.getMessage(), request);
    }

    // ===================== 私有工具方法 =====================

    /**
     * 构造统一的日志格式字符串。
     *
     * @return 占位符格式：type, code, status, message, uri, method, remote, traceId
     */
    private String buildLogFormat() {
        return "[EXCEPTION] type={}, code={}, status={}, message=\"{}\", uri={}, method={}, remote={}, traceId={}";
    }

    /**
     * 组装统一返回结构并附带 HTTP 状态码。
     *
     * @param status  HTTP 状态码
     * @param code    业务码（与统一返回结构中的 code 一致）
     * @param message 错误消息（优先使用异常 message）
     * @param request 当前请求（用于提取 traceId）
     * @return {@link ResponseEntity} 包裹的 {@link Result}
     */
    private ResponseEntity<Result<Void>> respond(final HttpStatus status,
                                                 final ApiCode code,
                                                 final String message,
                                                 final HttpServletRequest request) {
        Result<Void> body = Result.<Void>error(code, message)
                .setTraceId(resolveTraceId(request));
        return ResponseEntity.status(status).body(body);
    }

    /**
     * 从请求中解析 traceId。
     * 优先取请求头 {@link #TRACE_ID_HEADER}；可扩展为从 MDC 或 request attribute 获取。
     *
     * @param request 当前请求
     * @return traceId 字符串，可能为 null
     */
    private String resolveTraceId(final HttpServletRequest request) {
        String fromHeader = request.getHeader(TRACE_ID_HEADER);
        if (fromHeader != null && !fromHeader.isBlank())
            return fromHeader;

        Object attr = request.getAttribute("traceId");
        return attr == null ? null : String.valueOf(attr);
    }
}
