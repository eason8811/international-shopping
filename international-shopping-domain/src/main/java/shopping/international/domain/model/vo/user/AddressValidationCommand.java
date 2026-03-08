package shopping.international.domain.model.vo.user;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 地址校验命令
 */
public record AddressValidationCommand(String countryCode,
                                       String country,
                                       @Nullable String province,
                                       @Nullable String city,
                                       @Nullable String district,
                                       String addressLine1,
                                       @Nullable String addressLine2,
                                       @Nullable String zipcode,
                                       @Nullable String languageCode,
                                       @Nullable String rawInput,
                                       @Nullable String googlePlaceId,
                                       @Nullable Map<String, Object> placeResponse,
                                       @Nullable String previousResponseId) {
}
