package shopping.international.trigger.controller.orders;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.orders.DiscountCodeProductUpsertRequest;
import shopping.international.api.req.orders.DiscountCodeUpsertRequest;
import shopping.international.api.req.orders.DiscountPolicyUpsertRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.orders.DiscountAppliedViewRespond;
import shopping.international.api.resp.orders.DiscountCodeRespond;
import shopping.international.api.resp.orders.DiscountPolicyRespond;
import shopping.international.domain.model.aggregate.orders.DiscountCode;
import shopping.international.domain.model.aggregate.orders.DiscountPolicy;
import shopping.international.domain.model.enums.orders.DiscountApplyScope;
import shopping.international.domain.model.enums.orders.DiscountScopeMode;
import shopping.international.domain.model.enums.orders.DiscountStrategyType;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.orders.DiscountCodeSearchCriteria;
import shopping.international.domain.model.vo.orders.DiscountCodeText;
import shopping.international.domain.model.vo.orders.DiscountPolicySearchCriteria;
import shopping.international.domain.model.vo.orders.OrderDiscountAppliedSearchCriteria;
import shopping.international.domain.service.orders.IAdminDiscountService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.enums.ApiCode;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 管理侧折扣管理接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/admin")
public class AdminDiscountController {

    /**
     * 管理侧折扣领域服务
     */
    private final IAdminDiscountService adminDiscountService;

    /**
     * 查询折扣策略列表
     *
     * @param page         页码
     * @param size         每页大小
     * @param applyScope   作用域过滤 (可为空)
     * @param strategyType 类型过滤 (可为空)
     * @return 分页结果
     */
    @GetMapping("/discount-policies")
    public ResponseEntity<Result<List<DiscountPolicyRespond>>> listPolicies(@RequestParam(defaultValue = "1") int page,
                                                                            @RequestParam(defaultValue = "20") int size,
                                                                            @RequestParam(value = "name", required = false) String name,
                                                                            @RequestParam(value = "apply_scope", required = false) String applyScope,
                                                                            @RequestParam(value = "strategy_type", required = false) String strategyType) {
        PageQuery pageQuery = PageQuery.of(page, size, 500);
        DiscountApplyScope applyScopeEnum = parseEnum(applyScope, DiscountApplyScope.class);
        DiscountStrategyType strategyTypeEnum = parseEnum(strategyType, DiscountStrategyType.class);
        DiscountPolicySearchCriteria criteria = DiscountPolicySearchCriteria.builder()
                .name(name)
                .applyScope(applyScopeEnum)
                .strategyType(strategyTypeEnum)
                .build();
        criteria.validate();
        PageResult<DiscountPolicy> pageData = adminDiscountService.listPolicies(pageQuery, criteria);
        List<DiscountPolicyRespond> data = pageData.items().stream().map(AdminDiscountController::toRespond).toList();
        return ResponseEntity.ok(Result.ok(
                data,
                Result.Meta.builder()
                        .page(pageQuery.page())
                        .size(pageQuery.size())
                        .total(pageData.total())
                        .build()
        ));
    }

    /**
     * 创建折扣策略
     *
     * @param req 请求体
     * @return 已创建策略
     */
    @PostMapping("/discount-policies")
    public ResponseEntity<Result<DiscountPolicyRespond>> createPolicy(@RequestBody DiscountPolicyUpsertRequest req) {
        req.createValidate();

        BigDecimal percentOff = parseDecimalOrNull(req.getAmountOff(), "percentOff");
        BigDecimal amountOff = parseDecimalOrNull(req.getAmountOff(), "amountOff");
        BigDecimal minOrderAmount = parseDecimalOrNull(req.getMinOrderAmount(), "minOrderAmount");
        BigDecimal maxDiscountAmount = parseDecimalOrNull(req.getMaxDiscountAmount(), "maxDiscountAmount");

        DiscountPolicy created = adminDiscountService.createPolicy(
                DiscountPolicy.create(
                        req.getName(),
                        req.getApplyScope(),
                        req.getStrategyType(),
                        percentOff,
                        amountOff,
                        req.getCurrency(),
                        minOrderAmount,
                        maxDiscountAmount
                )
        );
        return ResponseEntity.status(ApiCode.CREATED.toHttpStatus())
                .body(Result.created(toRespond(created)));
    }

    /**
     * 更新折扣策略
     *
     * @param policyId 策略 ID
     * @param req      请求体
     * @return 更新后的策略
     */
    @PatchMapping("/discount-policies/{policy_id}")
    public ResponseEntity<Result<DiscountPolicyRespond>> updatePolicy(@PathVariable("policy_id") Long policyId,
                                                                      @RequestBody DiscountPolicyUpsertRequest req) {
        req.updateValidate();

        BigDecimal percentOff = parseDecimalOrNull(req.getAmountOff(), "percentOff");
        BigDecimal amountOff = parseDecimalOrNull(req.getAmountOff(), "amountOff");
        BigDecimal minOrderAmount = parseDecimalOrNull(req.getMinOrderAmount(), "minOrderAmount");
        BigDecimal maxDiscountAmount = parseDecimalOrNull(req.getMaxDiscountAmount(), "maxDiscountAmount");

        DiscountPolicy toUpdate = DiscountPolicy.create(
                req.getName(),
                req.getApplyScope(),
                req.getStrategyType(),
                percentOff,
                amountOff,
                req.getCurrency(),
                minOrderAmount,
                maxDiscountAmount
        );
        DiscountPolicy updated = adminDiscountService.updatePolicy(policyId, toUpdate);
        return ResponseEntity.ok(Result.ok(toRespond(updated)));
    }

    /**
     * 删除折扣策略
     *
     * @param policyId 策略 ID
     * @return 删除结果
     */
    @DeleteMapping("/discount-policies/{policy_id}")
    public ResponseEntity<Result<Void>> deletePolicy(@PathVariable("policy_id") Long policyId) {
        adminDiscountService.deletePolicy(policyId);
        return ResponseEntity.ok(Result.ok("折扣策略已删除"));
    }

    /**
     * 查询折扣码列表
     *
     * @param page        页码
     * @param size        每页大小
     * @param keyword     搜索关键词 (可为空)
     * @param policyId    策略 ID (可为空)
     * @param scopeMode   适用范围模式 (可为空)
     * @param expiresFrom 过期时间起 (可为空)
     * @param expiresTo   过期时间止 (可为空)
     * @return 分页结果
     */
    @GetMapping("/discount-codes")
    public ResponseEntity<Result<List<DiscountCodeRespond>>> listCodes(@RequestParam(defaultValue = "1") int page,
                                                                       @RequestParam(defaultValue = "20") int size,
                                                                       @RequestParam(required = false) String keyword,
                                                                       @RequestParam(value = "policy_id", required = false) Long policyId,
                                                                       @RequestParam(value = "scope_mode", required = false) String scopeMode,
                                                                       @RequestParam(value = "expires_from", required = false) String expiresFrom,
                                                                       @RequestParam(value = "expires_to", required = false) String expiresTo) {
        PageQuery pageQuery = PageQuery.of(page, size, 500);
        DiscountScopeMode scopeModeEnum = parseEnum(scopeMode, DiscountScopeMode.class);
        LocalDateTime from = parseDateTime(expiresFrom);
        LocalDateTime to = parseDateTime(expiresTo);
        DiscountCodeSearchCriteria criteria = DiscountCodeSearchCriteria.builder()
                .keyword(keyword)
                .policyId(policyId)
                .scopeMode(scopeModeEnum)
                .expiresFrom(from)
                .expiresTo(to)
                .build();
        criteria.validate();
        PageResult<DiscountCode> pageData = adminDiscountService.listCodes(pageQuery, criteria);
        List<DiscountCodeRespond> data = pageData.items().stream().map(AdminDiscountController::toRespond).toList();
        return ResponseEntity.ok(Result.ok(
                data,
                Result.Meta.builder()
                        .page(pageQuery.page())
                        .size(pageQuery.size())
                        .total(pageData.total())
                        .build()
        ));
    }

    /**
     * 创建折扣码
     *
     * @param req 请求体
     * @return 已创建折扣码
     */
    @PostMapping("/discount-codes")
    public ResponseEntity<Result<DiscountCodeRespond>> createCode(@RequestBody DiscountCodeUpsertRequest req) {
        req.createValidate();
        DiscountCodeText codeText = DiscountCodeText.ofNullable(req.getCode());
        if (codeText == null)
            throw new IllegalParamException("折扣码必须为 6 位大写字母数字");
        DiscountCode created = adminDiscountService.createCode(
                DiscountCode.create(
                        codeText,
                        req.getPolicyId(),
                        req.getName(),
                        req.getScopeMode(),
                        req.getExpiresAt()
                )
        );
        return ResponseEntity.status(ApiCode.CREATED.toHttpStatus()).body(Result.created(toRespond(created)));
    }

    /**
     * 更新折扣码
     *
     * @param codeId 折扣码 ID
     * @param req    请求体
     * @return 更新后的折扣码
     */
    @PatchMapping("/discount-codes/{code_id}")
    public ResponseEntity<Result<DiscountCodeRespond>> updateCode(@PathVariable("code_id") Long codeId,
                                                                  @RequestBody DiscountCodeUpsertRequest req) {
        req.updateValidate();
        DiscountCode updated = adminDiscountService.updateCode(codeId, code -> {
            if (req.getCode() != null) {
                String normalized = req.getCode().strip().toUpperCase();
                if (!normalized.equals(code.getCode().getValue()))
                    throw new ConflictException("折扣码不支持修改");
            }
            code.update(req.getPolicyId(), req.getName(), req.getScopeMode(), req.getExpiresAt());
        });
        return ResponseEntity.ok(Result.ok(toRespond(updated)));
    }

    /**
     * 删除折扣码
     *
     * @param codeId 折扣码 ID
     * @return 删除结果
     */
    @DeleteMapping("/discount-codes/{code_id}")
    public ResponseEntity<Result<Void>> deleteCode(@PathVariable("code_id") Long codeId) {
        adminDiscountService.deleteCode(codeId);
        return ResponseEntity.ok(Result.ok("折扣码已删除"));
    }

    /**
     * 获取折扣码适用商品映射 (SPU 列表)
     *
     * @param codeId 折扣码 ID
     * @return SPU ID 列表
     */
    @GetMapping("/discount-codes/{code_id}/products")
    public ResponseEntity<Result<List<Long>>> listCodeProducts(@PathVariable("code_id") Long codeId) {
        return ResponseEntity.ok(Result.ok(adminDiscountService.listCodeProducts(codeId)));
    }

    /**
     * 覆盖设置折扣码适用商品 (SPU 列表)
     *
     * @param codeId 折扣码 ID
     * @param req    请求体
     * @return 生效后的 SPU ID 列表
     */
    @PutMapping("/discount-codes/{code_id}/products")
    public ResponseEntity<Result<List<Long>>> replaceCodeProducts(@PathVariable("code_id") Long codeId,
                                                                  @RequestBody DiscountCodeProductUpsertRequest req) {
        req.validate();
        return ResponseEntity.ok(Result.ok(adminDiscountService.replaceCodeProducts(codeId, req.getProductIds())));
    }

    /**
     * 查询折扣实际使用流水
     *
     * @param page           页码
     * @param size           每页大小
     * @param orderNo        订单号 (可为空)
     * @param discountCodeId 折扣码 ID (可为空)
     * @param appliedScope   应用范围 (可为空)
     * @param from           时间起 (可为空)
     * @param to             时间止 (可为空)
     * @return 分页结果
     */
    @GetMapping("/order-discount-applications")
    public ResponseEntity<Result<List<DiscountAppliedViewRespond>>> listDiscountApplications(@RequestParam(defaultValue = "1") int page,
                                                                                             @RequestParam(defaultValue = "20") int size,
                                                                                             @RequestParam(value = "order_no", required = false) String orderNo,
                                                                                             @RequestParam(value = "discount_code_id", required = false) Long discountCodeId,
                                                                                             @RequestParam(value = "applied_scope", required = false) String appliedScope,
                                                                                             @RequestParam(value = "from", required = false) String from,
                                                                                             @RequestParam(value = "to", required = false) String to) {
        PageQuery pageQuery = PageQuery.of(page, size, 500);
        DiscountApplyScope appliedScopeEnum = parseEnum(appliedScope, DiscountApplyScope.class);
        OrderDiscountAppliedSearchCriteria criteria = OrderDiscountAppliedSearchCriteria.builder()
                .orderNo(orderNo)
                .discountCodeId(discountCodeId)
                .appliedScope(appliedScopeEnum)
                .from(parseDateTime(from))
                .to(parseDateTime(to))
                .build();
        criteria.validate();
        PageResult<IAdminDiscountService.OrderDiscountAppliedView> pageData = adminDiscountService.listOrderDiscountApplied(pageQuery, criteria);
        List<DiscountAppliedViewRespond> data = pageData.items().stream().map(v -> DiscountAppliedViewRespond.builder()
                .discountCodeId(v.discountCodeId())
                .orderItemId(v.orderItemId())
                .appliedScope(v.appliedScope())
                .appliedAmount(v.appliedAmount())
                .createdAt(v.createdAt())
                .build()).toList();
        return ResponseEntity.ok(Result.ok(
                data,
                Result.Meta.builder()
                        .page(pageQuery.page())
                        .size(pageQuery.size())
                        .total(pageData.total())
                        .build()
        ));
    }

    /**
     * DiscountPolicy → DiscountPolicyRespond
     *
     * @param policy 策略聚合
     * @return 响应
     */
    private static DiscountPolicyRespond toRespond(DiscountPolicy policy) {
        return DiscountPolicyRespond.builder()
                .id(policy.getId())
                .name(policy.getName())
                .applyScope(policy.getApplyScope())
                .strategyType(policy.getStrategyType())
                .percentOff(policy.getPercentOff() == null ? null : policy.getPercentOff().doubleValue())
                .amountOff(policy.getAmountOff() == null ? null : policy.getAmountOff().toPlainString())
                .currency(policy.getCurrency())
                .minOrderAmount(policy.getMinOrderAmount() == null ? null : policy.getMinOrderAmount().toPlainString())
                .maxDiscountAmount(policy.getMaxDiscountAmount() == null ? null : policy.getMaxDiscountAmount().toPlainString())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }

    /**
     * DiscountCode → DiscountCodeRespond
     *
     * @param code 折扣码聚合
     * @return 响应
     */
    private static DiscountCodeRespond toRespond(DiscountCode code) {
        return DiscountCodeRespond.builder()
                .id(code.getId())
                .code(code.getCode().getValue())
                .policyId(code.getPolicyId())
                .name(code.getName())
                .scopeMode(code.getScopeMode())
                .expiresAt(code.getExpiresAt())
                .createdAt(code.getCreatedAt())
                .updatedAt(code.getUpdatedAt())
                .build();
    }

    /**
     * 解析 ISO-8601 date-time 到 {@link LocalDateTime}
     *
     * @param value 字符串 (可为空)
     * @return LocalDateTime 或 null
     */
    private static @Nullable LocalDateTime parseDateTime(@Nullable String value) {
        if (value == null || value.isBlank())
            return null;
        String trimmed = value.strip();
        try {
            return OffsetDateTime.parse(trimmed).toLocalDateTime();
        } catch (Exception ignore) {
            try {
                return LocalDateTime.parse(trimmed);
            } catch (Exception ex) {
                throw new IllegalParamException("时间格式不合法: " + value);
            }
        }
    }

    /**
     * 解析枚举 (字符串为空返回 null)
     *
     * @param value    字符串
     * @param enumType 枚举类型
     * @param <E>      枚举泛型
     * @return 枚举或 null
     */
    private static <E extends Enum<E>> @Nullable E parseEnum(@Nullable String value, Class<E> enumType) {
        if (value == null || value.isBlank())
            return null;
        try {
            return Enum.valueOf(enumType, value.strip());
        } catch (Exception e) {
            throw new IllegalParamException("枚举值不合法: " + value);
        }
    }

    /**
     * 将金额字符串解析为 BigDecimal (可为空)
     *
     * @param raw       原始字符串
     * @param fieldName 字段名
     * @return BigDecimal 或 null
     */
    private static @Nullable BigDecimal parseDecimalOrNull(@Nullable String raw, String fieldName) {
        if (raw == null)
            return null;
        String trimmed = raw.strip();
        if (trimmed.isEmpty())
            return null;
        try {
            return new BigDecimal(trimmed).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            throw new IllegalParamException(fieldName + " 金额格式不合法");
        }
    }
}
