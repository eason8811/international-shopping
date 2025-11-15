package shopping.international.app.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import shopping.international.api.resp.Result;
import shopping.international.types.exceptions.*;
import shopping.international.types.enums.ApiCode;

/**
 * 全局异常处理器
 *
 * <p>职责: 
 * <ul>
 *   <li>拦截系统内常见异常并统一返回 {@link Result}</li>
 *   <li>根据异常类型设置合适的 HTTP 状态码与 {@link ApiCode}</li>
 *   <li>使用 {@code Slf4j} 输出统一格式的错误日志, 日志中包含错误原因, 请求基本信息与可用的 traceId</li>
 * </ul>
 * </p>
 *
 * <p>HTTP 状态与业务码约定: 
 * <ul>
 *   <li>{@link IllegalParamException} → 400 Bad Request / {@link ApiCode#BAD_REQUEST}</li>
 *   <li>{@link AppException} → 500 Internal Server Error / {@link ApiCode#INTERNAL_SERVER_ERROR}</li>
 *   <li>{@link RuntimeException} (兜底) → 500 Internal Server Error / {@link ApiCode#INTERNAL_SERVER_ERROR}</li>
 * </ul>
 * </p>
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * traceId 的请求头名称, 可根据网关/链路追踪系统调整
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * 处理邮件发送过多异常: 返回 429
     *
     * @param ex      抛出的 {@link TooManyEmailSentException}
     * @param request 当前请求
     * @return 统一返回结构 {@link Result}, HTTP 429
     */
    @ExceptionHandler(TooManyEmailSentException.class)
    public ResponseEntity<Result<Void>> handleIllegalParam(final TooManyEmailSentException ex,
                                                           final HttpServletRequest request) {
        // 统一格式日志 (参数类异常一般不需要打印堆栈, 避免噪音)
        log.warn(buildLogFormat(),
                ex.getClass().getSimpleName(),
                ApiCode.TOO_MANY_REQUESTS,
                HttpStatus.TOO_MANY_REQUESTS.value(),
                ex.getMessage(),
                request.getRequestURI(),
                request.getMethod(),
                request.getRemoteAddr(),
                resolveTraceId(request));

        return respond(HttpStatus.TOO_MANY_REQUESTS, ApiCode.TOO_MANY_REQUESTS, ex.getMessage(), request);
    }

    /**
     * 处理用户账号异常: 返回 401
     *
     * @param ex      抛出的 {@link AccountException}
     * @param request 当前请求
     * @return 统一返回结构 {@link Result}, HTTP 401
     */
    @ExceptionHandler(AccountException.class)
    public ResponseEntity<Result<Void>> handleIllegalParam(final AccountException ex,
                                                           final HttpServletRequest request) {
        // 统一格式日志 (参数类异常一般不需要打印堆栈, 避免噪音)
        log.warn(buildLogFormat(),
                ex.getClass().getSimpleName(),
                ApiCode.UNAUTHORIZED,
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage(),
                request.getRequestURI(),
                request.getMethod(),
                request.getRemoteAddr(),
                resolveTraceId(request));

        return respond(HttpStatus.UNAUTHORIZED, ApiCode.UNAUTHORIZED, ex.getMessage(), request);
    }

    /**
     * 处理验证码不合法异常: 返回 422
     *
     * @param ex      抛出的 {@link VerificationCodeInvalidException}
     * @param request 当前请求
     * @return 统一返回结构 {@link Result}, HTTP 422
     */
    @ExceptionHandler(VerificationCodeInvalidException.class)
    public ResponseEntity<Result<Void>> handleIllegalParam(final VerificationCodeInvalidException ex,
                                                           final HttpServletRequest request) {
        // 统一格式日志 (参数类异常一般不需要打印堆栈, 避免噪音)
        log.warn(buildLogFormat(),
                ex.getClass().getSimpleName(),
                ApiCode.UNPROCESSABLE_ENTITY,
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                ex.getMessage(),
                request.getRequestURI(),
                request.getMethod(),
                request.getRemoteAddr(),
                resolveTraceId(request));

        return respond(HttpStatus.UNPROCESSABLE_ENTITY, ApiCode.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    /**
     * 处理冲突异常: 返回 409
     *
     * @param ex      抛出的 {@link ConflictException}
     * @param request 当前请求
     * @return 统一返回结构 {@link Result}, HTTP 409
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Result<Void>> handleIllegalParam(final ConflictException ex,
                                                           final HttpServletRequest request) {
        // 统一格式日志 (参数类异常一般不需要打印堆栈, 避免噪音)
        log.warn(buildLogFormat(),
                ex.getClass().getSimpleName(),
                ApiCode.CONFLICT,
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                request.getRequestURI(),
                request.getMethod(),
                request.getRemoteAddr(),
                resolveTraceId(request));

        return respond(HttpStatus.CONFLICT, ApiCode.CONFLICT, ex.getMessage(), request);
    }


    /**
     * 处理参数错误异常: 返回 400
     *
     * @param ex      抛出的 {@link IllegalParamException} 或 {@link RefreshTokenInvalidException}
     * @param request 当前请求
     * @return 统一返回结构 {@link Result}, HTTP 400
     */
    @ExceptionHandler({IllegalParamException.class, RefreshTokenInvalidException.class})
    public ResponseEntity<Result<Void>> handleIllegalParam(final Exception ex,
                                                           final HttpServletRequest request) {
        // 统一格式日志 (参数类异常一般不需要打印堆栈, 避免噪音)
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
     * 处理系统内自定义异常: 返回 500
     *
     * @param ex      抛出的 {@link AppException}
     * @param request 当前请求
     * @return 统一返回结构 {@link Result}, HTTP 500
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<Result<Void>> handleAppException(final AppException ex,
                                                           final HttpServletRequest request) {
        // 系统异常打印堆栈, 便于排查
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
     * 处理未显式捕获的运行时异常 (兜底): 返回 500
     *
     * @param ex      抛出的 {@link RuntimeException}
     * @param request 当前请求
     * @return 统一返回结构 {@link Result}, HTTP 500
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



    /**
     * 处理缺少 Servlet 请求参数的异常, 返回 400 错误
     *
     * @param ex 抛出的 {@link MissingServletRequestParameterException} 异常
     * @param headers 响应头, 通常用于设置额外的 HTTP 头信息
     * @param status HTTP 状态码, 对于此类异常固定为 400 (Bad Request)
     * @param request 当前 Web 请求, 用于获取请求相关信息如 HTTP 方法等
     * @return 统一返回结构 {@link ResponseEntity} 包含了 {@link Result} 和 HTTP 400 状态码
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          @NotNull HttpHeaders headers,
                                                                          @NotNull HttpStatusCode status,
                                                                          @NotNull WebRequest request) {
        // 统一格式日志 (参数类异常一般不需要打印堆栈, 避免噪音)
        log.warn(buildLogFormat(),
                ex.getClass().getSimpleName(),
                ApiCode.BAD_REQUEST,
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                null,
                ((ServletWebRequest) request).getHttpMethod(),
                null,
                resolveTraceId(null));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.<Void>error(ApiCode.BAD_REQUEST, ex.getMessage()));
    }

    /**
     * 处理资源未找到异常, 返回 404 错误
     *
     * <p>此方法用于统一处理 {@link NoResourceFoundException}, 当请求的资源不存在时触发。该方法首先记录一条警告级别的日志,
     * 随后返回一个包含错误信息的统一响应结构, HTTP 状态码为 404 (Not Found)</p>
     *
     * @param ex 抛出的 {@link NoResourceFoundException} 异常, 包含了关于未能找到资源的具体信息
     * @param headers 响应头, 通常用于设置额外的 HTTP 头信息
     * @param status HTTP 状态码, 对于此类异常固定为 404 (Not Found)
     * @param request 当前 Web 请求, 用于获取请求相关信息如 HTTP 方法等
     * @return 统一返回结构 {@link ResponseEntity} 包含了 {@link Result} 和 HTTP 404 状态码
     */
    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex,
                                                                    @NotNull HttpHeaders headers,
                                                                    @NotNull HttpStatusCode status,
                                                                    @NotNull WebRequest request) {
        // 统一格式日志 (参数类异常一般不需要打印堆栈, 避免噪音)
        log.warn(buildLogFormat(),
                ex.getClass().getSimpleName(),
                ApiCode.NOT_FOUND,
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                null,
                ((ServletWebRequest) request).getHttpMethod(),
                null,
                resolveTraceId(null));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.<Void>error(ApiCode.NOT_FOUND, ex.getMessage()));
    }

    // ===================== 私有工具方法 =====================

    /**
     * 构造统一的日志格式字符串
     *
     * @return 占位符格式: type, code, status, message, uri, method, remote, traceId
     */
    private String buildLogFormat() {
        return "[EXCEPTION] type={}, code={}, status={}, message=\"{}\", uri={}, method={}, remote={}, traceId={}";
    }

    /**
     * 组装统一返回结构并附带 HTTP 状态码
     *
     * @param status  HTTP 状态码
     * @param code    业务码 (与统一返回结构中的 code 一致)
     * @param message 错误消息 (优先使用异常 message)
     * @param request 当前请求 (用于提取 traceId)
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
     * 从请求中解析 traceId
     * 优先取请求头 {@link #TRACE_ID_HEADER}, 可扩展为从 MDC 或 request attribute 获取
     *
     * @param request 当前请求
     * @return traceId 字符串, 可能为 null
     */
    private String resolveTraceId(final HttpServletRequest request) {
        if (request == null)
            return null;
        String fromHeader = request.getHeader(TRACE_ID_HEADER);
        if (fromHeader != null && !fromHeader.isBlank())
            return fromHeader;

        Object attr = request.getAttribute("traceId");
        return attr == null ? null : String.valueOf(attr);
    }
}
