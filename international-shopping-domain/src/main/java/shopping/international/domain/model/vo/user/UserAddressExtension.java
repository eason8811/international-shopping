package shopping.international.domain.model.vo.user;

import lombok.*;

import java.math.BigDecimal;

/**
 * 收货地址扩展快照
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class UserAddressExtension {
    private String googlePlaceId;
    private String formattedAddress;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String rawInput;
    private String postalAddressJson;
    private String placeResponseJson;
    private String validationResponseId;
    private String validationGranularity;
    private String geocodeGranularity;
    private Boolean addressComplete;
    private String possibleNextAction;
    private String validationResponseJson;
}
