package shopping.international.trigger.controller.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.enums.ApiCode;
import shopping.international.types.exceptions.AccountException;
import shopping.international.types.exceptions.IdempotencyException;

import java.util.List;

/**
 * 收货地址接口 {@code /users/me/addresses}
 *
 * <p>职责：
 * <ul>
 *   <li>列表, 详情</li>
 *   <li>新增, 修改, 删除</li>
 *   <li>设置默认地址 (同用户仅一个)</li>
 * </ul>
 * </p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/users/me/addresses")
public class AddressController {

    /**
     * 地址领域服务
     */
    private final IAddressService addressService;


    /**
     * 获取当前登录用户的收货地址列表, 分页显示
     *
     * @param page 页码, 默认值为 1
     * @param size 每页显示的条目数, 默认值为 5
     * @return 包含分页信息和地址列表的结果集
     */
    @GetMapping
    public ResponseEntity<Result<List<AddressRespond>>> list(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "5") int size) {
        if (size > 50)
            size = 50;
        Long uid = requireCurrentUserId();
        IAddressService.PageResult pageData = addressService.list(uid, page, size);
        List<AddressRespond> data = pageData.items().stream().map(AddressRespond::from).toList();
        return ResponseEntity.ok(Result.ok(
                data,
                Result.Meta.builder()
                        .page(page)
                        .size(size)
                        .total(pageData.total())
                        .build()
        ));
    }

    /**
     * 创建新的收货地址
     *
     * <p>该方法用于用户创建一个新的收货地址, 需要提供接收者姓名, 电话号码, 国家, 省份, 城市, 区县等信息, 可以设置此地址是否为默认地址,
     * 通过 {@code Idempotency-Key} 请求头保证操作的幂等性</p>
     *
     * @param idempotencyKey 幂等键, 用于确保客户端多次发送相同的请求时服务器只处理一次, 防止重复创建
     * @param req            包含新地址详细信息的请求对象
     * @return 包含刚创建的地址信息的结果集, 如果成功创建, 返回状态码为 201 Created
     */
    @PostMapping
    public ResponseEntity<Result<AddressRespond>> create(@RequestHeader(value = "Idempotency-Key") String idempotencyKey,
                                                         @RequestBody @Valid CreateAddressRequest req) {
        req.validate();
        Long uid = requireCurrentUserId();

        PhoneNumber phoneNumber = PhoneNumber.of(req.getPhone());
        UserAddress newAddress = UserAddress.of(req.getReceiverName(), phoneNumber, req.getCountry(),
                req.getProvince(), req.getCity(), req.getDistrict(), req.getAddressLine1(), req.getAddressLine2(),
                req.getZipcode(), Boolean.TRUE.equals(req.getIsDefault()));

        try {
            UserAddress created = addressService.create(uid, newAddress, idempotencyKey);
            return ResponseEntity.status(ApiCode.CREATED.toHttpStatus())
                    .body(Result.created(AddressRespond.from(created)));
        } catch (IdempotencyException ignore) {
            log.warn("参数为 uid={}, newAddress={}, idempotencyKey={} 的请求已处理过, 忽略本次请求", uid, newAddress, idempotencyKey);
            return ResponseEntity.status(ApiCode.ACCEPTED.toHttpStatus())
                    .body(Result.accepted("该地址已提交"));
        }
    }

    /**
     * 根据指定的 ID 获取当前登录用户的单个收货地址信息
     *
     * @param id 收货地址的唯一标识符
     * @return 包含请求地址详细信息的结果集, 如果成功找到, 返回状态码为 200 OK
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<AddressRespond>> get(@PathVariable("id") Long id) {
        Long uid = requireCurrentUserId();
        UserAddress address = addressService.get(uid, id);
        return ResponseEntity.ok(Result.ok(AddressRespond.from(address)));
    }

    /**
     * 更新指定ID的收货地址信息
     *
     * <p>该方法允许用户更新已存在的收货地址, 包括接收者姓名, 电话号码, 国家, 省份, 城市, 区县等信息, 同时可以设置此地址是否为默认地址
     * 请求体中需包含 {@link UpdateAddressRequest} 对象, 用于传递新的地址详情</p>
     *
     * @param id  地址的唯一标识符
     * @param req 包含要更新的地址详细信息的请求对象
     * @return 包含更新后的地址信息的结果集, 如果成功更新, 返回状态码为 200 OK
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Result<AddressRespond>> update(@PathVariable("id") Long id, @RequestBody UpdateAddressRequest req) {
        req.validate();
        Long uid = requireCurrentUserId();

        PhoneNumber phone = req.getPhone() == null ? null : PhoneNumber.nullableOf(req.getPhone());
        UserAddress updated = addressService.update(uid, id, req.getReceiverName(), phone, req.getCountry(),
                req.getProvince(), req.getCity(), req.getDistrict(), req.getAddressLine1(), req.getAddressLine2(),
                req.getZipcode(), req.getIsDefault());
        return ResponseEntity.ok(Result.ok(AddressRespond.from(updated), "地址已更新"));
    }

    /**
     * 删除指定ID的收货地址
     *
     * <p>此方法允许用户删除一个已存在的收货地址, 通过提供地址的唯一标识符 {@code id} 来执行删除操作, 删除后, 返回一个确认消息表示该地址已被成功删除</p>
     *
     * @param id 地址的唯一标识符, 用于定位要删除的具体地址
     * @return 包含删除结果的消息, 如果删除成功, 将返回状态码为 200 OK 的响应, 并携带确认消息 "地址已删除"
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> delete(@PathVariable("id") Long id) {
        Long uid = requireCurrentUserId();
        addressService.delete(uid, id);
        return ResponseEntity.ok(Result.ok("地址已删除"));
    }

    /**
     * 设置指定ID的地址为默认收货地址
     *
     * <p>此方法允许用户将一个已存在的收货地址设置为默认地址, 通过提供地址的唯一标识符 {@code id} 来执行设置操作,
     * 同一用户只能有一个默认地址, 如果之前已经存在默认地址, 则新的设置会覆盖旧的默认地址, </p>
     *
     * @param id 地址的唯一标识符, 用于定位要设置为默认的具体地址
     * @return 包含设置结果的消息, 如果成功设置, 将返回状态码为 200 OK 的响应, 并携带确认消息 "默认地址已设置"
     */
    @PostMapping("/{id}/set-default")
    public ResponseEntity<Result<Void>> setDefault(@PathVariable("id") Long id) {
        Long uid = requireCurrentUserId();
        addressService.setDefault(uid, id);
        return ResponseEntity.ok(Result.ok("默认地址已设置"));
    }

    /**
     * 从安全上下文中解析当前用户ID, 解析失败则抛出未登录异常
     *
     * @return 当前登录用户ID
     * @throws AccountException 如果用户未登录或无法解析当前用户信息时抛出
     */
    private Long requireCurrentUserId() {
        Authentication authentication = null;
        if (SecurityContextHolder.getContext() != null)
            authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            throw new AccountException("未登录");
        Object principal = authentication.getPrincipal();
        // 典型实现: principal 可为自定义对象, 需从中提取 id, 此处兼容 Long 和 Map-like 的简单示例
        if (principal instanceof Long longUserId)
            return longUserId;
        if (principal instanceof String stringUserId)
            return Long.parseLong(stringUserId);
        throw new AccountException("无法解析当前用户");
    }
}
