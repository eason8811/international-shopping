package shopping.international.infrastructure.dao.user.po;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 持久化对象: user_address
 * <p>用户收货地址 1:N</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
@Table("user_address")
public class UserAddressPO {
    /**
     * 主键ID (自增) 
     */
    @Id(keyType = KeyType.Auto)
    @Column("id")
    private Long id;
    /**
     * 用户ID
     */
    @Column("user_id")
    private Long userId;
    /**
     * 收货人
     */
    @Column("receiver_name")
    private String receiverName;
    /**
     * 联系电话 (字符串持久化) 
     */
    @Column("phone")
    private String phone;
    /**
     * 国家
     */
    @Column("country")
    private String country;
    /**
     * 省/州
     */
    @Column("province")
    private String province;
    /**
     * 城市
     */
    @Column("city")
    private String city;
    /**
     * 区/县
     */
    @Column("district")
    private String district;
    /**
     * 地址行1
     */
    @Column("address_line1")
    private String addressLine1;
    /**
     * 地址行2
     */
    @Column("address_line2")
    private String addressLine2;
    /**
     * 邮编
     */
    @Column("zipcode")
    private String zipcode;
    /**
     * 是否默认地址
     */
    @Column("is_default")
    private Boolean isDefault;
    /**
     * 创建时间
     */
    @Column("created_at")
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
