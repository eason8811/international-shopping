package shopping.international.trigger.controller.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.user.CreateAddressRequest;
import shopping.international.api.req.user.UpdateAddressRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.user.AddressRespond;
import shopping.international.domain.model.entity.user.UserAddress;
import shopping.international.domain.model.vo.user.PhoneNumber;
import shopping.international.domain.service.user.IAddressService;
import shopping.international.types.enums.ApiCode;
import shopping.international.types.exceptions.AccountException;

import java.util.List;

/**
 * 收货地址接口（/users/me/addresses）
 *
 * <p>职责：
 * <ul>
 *   <li>列表、详情</li>
 *   <li>新增、修改、删除</li>
 *   <li>设置默认地址（同用户仅一个）</li>
 * </ul>
 * </p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/addresses")
public class AddressController {

    /**
     * 地址领域服务
     */
    private final IAddressService addressService;

    /**
     * 列出当前用户的收货地址（简单分页）
     *
     * @param page 页码（从1开始）
     * @param size 页大小
     * @return 列表 + meta
     */
    @GetMapping
    public ResponseEntity<Result<List<AddressRespond>>> list(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        Long uid = requireCurrentUserId();
        var pageData = addressService.list(uid, page, size);
        List<AddressRespond> data = pageData.items().stream().map(AddressRespond::from).toList();
        return ResponseEntity.ok(Result.ok(data,
                Result.Meta.builder().page(page).size(size).total(pageData.total()).build()));
    }

    /**
     * 新增收货地址（支持 Idempotency-Key）
     *
     * @param idempotencyKey 幂等键（可选）
     * @param req            新增请求
     * @return 已创建的地址
     */
    @PostMapping
    public ResponseEntity<Result<AddressRespond>> create(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                         @RequestBody @Valid CreateAddressRequest req) {
        req.validate();
        Long uid = requireCurrentUserId();

        PhoneNumber phone = PhoneNumber.of(req.getPhone());
        UserAddress created = addressService.create(uid, req.getReceiverName(),
                phone, req.getCountry(), req.getProvince(), req.getCity(), req.getDistrict(),
                req.getAddressLine1(), req.getAddressLine2(), req.getZipcode(), Boolean.TRUE.equals(req.getIsDefault()),
                idempotencyKey);
        return ResponseEntity.status(ApiCode.CREATED.toHttpStatus())
                .body(Result.created(AddressRespond.from(created)));
    }

    /**
     * 获取收货地址详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<AddressRespond>> get(@PathVariable("id") Long id) {
        Long uid = requireCurrentUserId();
        UserAddress address = addressService.get(uid, id);
        return ResponseEntity.ok(Result.ok(AddressRespond.from(address)));
    }

    /**
     * 修改收货地址
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Result<AddressRespond>> update(@PathVariable("id") Long id,
                                                         @RequestBody @Valid UpdateAddressRequest req) {
        req.validate();
        Long uid = requireCurrentUserId();

        PhoneNumber phone = req.getPhone() == null ? null : PhoneNumber.nullableOf(req.getPhone());
        UserAddress updated = addressService.update(uid, id, req.getReceiverName(), phone,
                req.getCountry(), req.getProvince(), req.getCity(), req.getDistrict(),
                req.getAddressLine1(), req.getAddressLine2(), req.getZipcode(), req.getIsDefault());
        return ResponseEntity.ok(Result.ok(AddressRespond.from(updated), "Address updated"));
    }

    /**
     * 删除收货地址
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> delete(@PathVariable("id") Long id) {
        Long uid = requireCurrentUserId();
        addressService.delete(uid, id);
        return ResponseEntity.ok(Result.ok("Address deleted"));
    }

    /**
     * 设为默认地址
     */
    @PostMapping("/{id}/set-default")
    public ResponseEntity<Result<Void>> setDefault(@PathVariable("id") Long id) {
        Long uid = requireCurrentUserId();
        addressService.setDefault(uid, id);
        return ResponseEntity.ok(Result.ok("Default address set"));
    }

    private Long requireCurrentUserId() {
        Authentication a = SecurityContextHolder.getContext() == null ? null : SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated())
            throw new AccountException("未登录");
        Object principal = a.getPrincipal();
        if (principal instanceof Long l) return l;
        if (principal instanceof shopping.international.types.security.LoginPrincipal p) return p.id();
        throw new AccountException("无法解析当前用户");
    }
}
