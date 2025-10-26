package shopping.international.types.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.EnumSet;

/**
 * 统一返回结构中的业务码
 * <p>业务码并非 HTTP 状态码, 两者在语义上相关但不等价</p>
 */
@Getter
@JsonFormat(shape = JsonFormat.Shape.STRING) // 序列化为字符串，如 "OK"
public enum ApiCode {

    // ===== 成功类 =====
    /**
     * 200 OK
     */
    OK(HttpStatus.OK, "OK"),
    /**
     * 201 Created
     */
    CREATED(HttpStatus.CREATED, "Created"),
    /**
     * 202 Accepted
     */
    ACCEPTED(HttpStatus.ACCEPTED, "Accepted"),
    /**
     * 302 Found / Redirect
     */
    FOUND(HttpStatus.FOUND, "Found / Redirect"),

    // ===== 客户端错误类 =====
    /**
     * 400 Bad Request
     */
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad Request"),
    /**
     * 401 Unauthorized
     */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized"),
    /**
     * 404 Not Found
     */
    NOT_FOUND(HttpStatus.NOT_FOUND, "Not Found"),
    /**
     * 409 Conflict
     */
    CONFLICT(HttpStatus.CONFLICT, "Conflict"),
    /**
     * 422 Unprocessable Entity
     */
    UNPROCESSABLE_ENTITY(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity");

    private final int httpStatus;
    private final String defaultMessage;

    ApiCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus.value();
        this.defaultMessage = defaultMessage;
    }

    /**
     * 业务是否 成功
     *
     * @return true 业务成功, false 业务失败
     */
    public boolean isSuccess() {
        return EnumSet.of(OK, CREATED, ACCEPTED, FOUND).contains(this);
    }

    /**
     * 根据 Spring 的 HttpStatus 映射为业务码
     *
     * @param status Spring 的 HttpStatus
     * @return 业务码
     */
    public static ApiCode fromHttpStatus(HttpStatus status) {
        return switch (status) {
            case OK -> OK;
            case CREATED -> CREATED;
            case ACCEPTED -> ACCEPTED;
            case FOUND -> FOUND;
            case BAD_REQUEST -> BAD_REQUEST;
            case UNAUTHORIZED -> UNAUTHORIZED;
            case NOT_FOUND -> NOT_FOUND;
            case CONFLICT -> CONFLICT;
            case UNPROCESSABLE_ENTITY -> UNPROCESSABLE_ENTITY;
            default -> throw new IllegalArgumentException("Unsupported HttpStatus: " + status);
        };
    }

    /**
     * 将业务码转回 Spring 的 HttpStatus（便于 Controller 设定 HTTP 状态）
     *
     * @return Spring 的 HttpStatus
     */
    public HttpStatus toHttpStatus() {
        return HttpStatus.valueOf(this.httpStatus);
    }
}
