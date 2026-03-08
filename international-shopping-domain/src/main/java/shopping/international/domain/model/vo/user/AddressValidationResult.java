package shopping.international.domain.model.vo.user;

import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.user.AddressValidationStatus;

/**
 * 地址校验结果
 */
public record AddressValidationResult(String countryCode,
                                      String country,
                                      @Nullable String province,
                                      @Nullable String city,
                                      @Nullable String district,
                                      String addressLine1,
                                      @Nullable String addressLine2,
                                      @Nullable String zipcode,
                                      @Nullable String languageCode,
                                      AddressValidationStatus validationStatus,
                                      @Nullable String possibleNextAction,
                                      UserAddressExtension extension) {
}
