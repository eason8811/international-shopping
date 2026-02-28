package shopping.international.domain.model.enums.customerservice;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 补发状态枚举, 对齐表 `aftersales_reship.status`, 并内聚状态机流转规则
 * <ul>
 *     <li>{@code INIT:} 初始化</li>
 *     <li>{@code APPROVED:} 已审批</li>
 *     <li>{@code FULFILLING:} 履约中</li>
 *     <li>{@code FULFILLED:} 履约完成</li>
 *     <li>{@code CANCELLED:} 已取消</li>
 * </ul>
 */
public enum ReshipStatus {
    /**
     * 初始化
     */
    INIT,
    /**
     * 已审批
     */
    APPROVED,
    /**
     * 履约中
     */
    FULFILLING,
    /**
     * 履约完成
     */
    FULFILLED,
    /**
     * 已取消
     */
    CANCELLED;

    /**
     * 将字符串转换为补发状态枚举
     *
     * @param value 原始字符串值
     * @return 对应的补发状态枚举
     */
    public static ReshipStatus fromValue(String value) {
        requireNotBlank(value, "reshipStatus 不能为空");
        return ReshipStatus.valueOf(value.strip().toUpperCase(Locale.ROOT));
    }

    /**
     * 判断当前状态是否允许流转到目标状态
     *
     * @param toStatus 目标状态
     * @return 若允许流转, 返回 true
     */
    public boolean canTransitTo(@NotNull ReshipStatus toStatus) {
        requireNotNull(toStatus, "toStatus 不能为空");
        if (this == toStatus)
            return false;
        return switch (this) {
            case INIT -> toStatus == APPROVED || toStatus == CANCELLED;
            case APPROVED -> toStatus == FULFILLING || toStatus == CANCELLED;
            case FULFILLING -> toStatus == FULFILLED;
            case FULFILLED, CANCELLED -> false;
        };
    }

    /**
     * 判断当前状态是否为终态
     *
     * @return 若为终态, 返回 true
     */
    public boolean isFinalStatus() {
        return this == FULFILLED || this == CANCELLED;
    }
}
