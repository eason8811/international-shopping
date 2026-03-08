package shopping.international.infrastructure.dao.user.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 持久化对象: user_address_ext
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
@TableName("user_address_ext")
public class UserAddressExtPO {
    @TableId(value = "address_id", type = IdType.INPUT)
    private Long addressId;
    @TableField("google_place_id")
    private String googlePlaceId;
    @TableField("formatted_address")
    private String formattedAddress;
    @TableField("latitude")
    private BigDecimal latitude;
    @TableField("longitude")
    private BigDecimal longitude;
    @TableField("raw_input")
    private String rawInput;
    @TableField("postal_address_json")
    private String postalAddressJson;
    @TableField("place_response_json")
    private String placeResponseJson;
    @TableField("validation_response_id")
    private String validationResponseId;
    @TableField("validation_granularity")
    private String validationGranularity;
    @TableField("geocode_granularity")
    private String geocodeGranularity;
    @TableField("address_complete")
    private Boolean addressComplete;
    @TableField("possible_next_action")
    private String possibleNextAction;
    @TableField("validation_response_json")
    private String validationResponseJson;
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
