package shopping.international.domain.model.vo.user;

import lombok.Builder;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.user.AddressSource;

import java.util.Map;

/**
 * 地址创建/修改命令
 */
@Builder(toBuilder = true)
public record AddressChangeCommand(@Nullable String receiverName,
                                   @Nullable PhoneNumber phone,
                                   @Nullable String countryCode,
                                   @Nullable String country,
                                   @Nullable String province,
                                   @Nullable String city,
                                   @Nullable String district,
                                   @Nullable String addressLine1,
                                   @Nullable String addressLine2,
                                   @Nullable String zipcode,
                                   @Nullable String languageCode,
                                   @Nullable AddressSource addressSource,
                                   @Nullable Boolean makeDefault,
                                   @Nullable String rawInput,
                                   @Nullable String googlePlaceId,
                                   @Nullable Map<String, Object> placeResponse) {
}
