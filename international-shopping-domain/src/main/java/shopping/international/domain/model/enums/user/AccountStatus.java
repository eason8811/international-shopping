package shopping.international.domain.model.enums.user;

import lombok.Getter;

/**
 * 账户状态，与表 user_account.status 一致。
 */
@Getter
public enum AccountStatus {
    /** 已激活，可登录 */
    ACTIVE,
    /** 未激活或禁用 */
    DISABLED
}
