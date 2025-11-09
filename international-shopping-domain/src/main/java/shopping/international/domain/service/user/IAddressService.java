package shopping.international.domain.service.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.entity.user.UserAddress;
import shopping.international.domain.model.vo.user.PhoneNumber;

import java.util.List;

/**
 * 收货地址领域服务接口
 */
public interface IAddressService {

    /**
     * 简单分页返回
     */
    record PageResult(List<UserAddress> items, long total) {
    }

    /**
     * 列出当前用户地址
     *
     * @param userId 用户ID
     * @param page   页码 (1起)
     * @param size   大小
     */
    @NotNull
    PageResult list(@NotNull Long userId, int page, int size);

    /**
     * 获取详情 (需属于本人)
     */
    @NotNull
    UserAddress get(@NotNull Long userId, @NotNull Long addressId);

    /**
     * 新增地址 (支持幂等键)
     */
    @NotNull
    UserAddress create(@NotNull Long userId,
                       @NotNull String receiverName,
                       @NotNull PhoneNumber phone,
                       @Nullable String country,
                       @Nullable String province,
                       @Nullable String city,
                       @Nullable String district,
                       @NotNull String addressLine1,
                       @Nullable String addressLine2,
                       @Nullable String zipcode,
                       boolean isDefault,
                       @Nullable String idempotencyKey);

    /**
     * 修改地址
     */
    @NotNull
    UserAddress update(@NotNull Long userId,
                       @NotNull Long addressId,
                       @Nullable String receiverName,
                       @Nullable PhoneNumber phone,
                       @Nullable String country,
                       @Nullable String province,
                       @Nullable String city,
                       @Nullable String district,
                       @Nullable String addressLine1,
                       @Nullable String addressLine2,
                       @Nullable String zipcode,
                       @Nullable Boolean makeDefault);

    /**
     * 删除地址
     */
    void delete(@NotNull Long userId, @NotNull Long addressId);

    /**
     * 设为默认
     */
    void setDefault(@NotNull Long userId, @NotNull Long addressId);
}
