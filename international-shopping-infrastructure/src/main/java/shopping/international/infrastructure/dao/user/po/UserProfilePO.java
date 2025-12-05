package shopping.international.infrastructure.dao.user.po;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 持久化对象：user_profile (record)
 * <p>用户资料 1:1；主键为 user_id, 非自增</p>
 *
 * <ol>
 *     <li>{@code userId: }用户ID (主键) </li>
 *     <li>{@code displayName: }显示名</li>
 *     <li>{@code avatarUrl: }头像URL</li>
 *     <li>{@code gender: }性别 (UNKNOWN/MALE/FEMALE) </li>
 *     <li>{@code birthday: }生日</li>
 *     <li>{@code country: }国家</li>
 *     <li>{@code province: }省/州</li>
 *     <li>{@code city: }城市</li>
 *     <li>{@code addressLine: }地址(简单场景)</li>
 *     <li>{@code zipcode: }邮编</li>
 *     <li>{@code extra: }扩展信息(JSON)</li>
 *     <li>{@code createdAt: }创建时间</li>
 *     <li>{@code updatedAt: }更新时间</li>
 * </ol>
 *
 * <p>注意：record 字段不可回填主键, 自增不适用；本表符合 record 的不可变场景</p>
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_profile")
public final class UserProfilePO {
    @Id(keyType = KeyType.None)
    @Column("user_id")
    private Long userId;
    @Column("display_name")
    private String displayName;
    @Column("avatar_url")
    private String avatarUrl;
    @Column("gender")
    private String gender;
    @Column("birthday")
    private LocalDate birthday;
    @Column("country")
    private String country;
    @Column("province")
    private String province;
    @Column("city")
    private String city;
    @Column("address_line")
    private String addressLine;
    @Column("zipcode")
    private String zipcode;
    @Column("extra")
    private String extra;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;

}
