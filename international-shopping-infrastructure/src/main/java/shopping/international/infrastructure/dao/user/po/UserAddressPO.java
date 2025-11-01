package shopping.international.infrastructure.dao.user.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 持久化对象: user_address
 * <p>用户收货地址 1:N</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_address")
public class UserAddressPO {
    /**
     * 主键ID (自增) 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;
    /**
     * 收货人
     */
    @TableField("receiver_name")
    private String receiverName;
    /**
     * 联系电话 (字符串持久化) 
     */
    @TableField("phone")
    private String phone;
    /**
     * 国家
     */
    @TableField("country")
    private String country;
    /**
     * 省/州
     */
    @TableField("province")
    private String province;
    /**
     * 城市
     */
    @TableField("city")
    private String city;
    /**
     * 区/县
     */
    @TableField("district")
    private String district;
    /**
     * 地址行1
     */
    @TableField("address_line1")
    private String addressLine1;
    /**
     * 地址行2
     */
    @TableField("address_line2")
    private String addressLine2;
    /**
     * 邮编
     */
    @TableField("zipcode")
    private String zipcode;
    /**
     * 是否默认地址
     */
    @TableField("is_default")
    private Boolean isDefault;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
