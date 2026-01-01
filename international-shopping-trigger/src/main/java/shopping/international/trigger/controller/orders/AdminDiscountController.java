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
import shopping.international.domain.model.entity.orders.DiscountPolicyAmount;
import shopping.international.domain.model.enums.orders.DiscountApplyScope;
import shopping.international.domain.model.enums.orders.DiscountScopeMode;
import shopping.international.domain.model.enums.orders.DiscountStrategyType;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.orders.DiscountCodeSearchCriteria;
import shopping.international.domain.model.vo.orders.DiscountCodeText;
import shopping.international.domain.model.vo.orders.DiscountPolicySearchCriteria;
import shopping.international.domain.model.vo.orders.OrderDiscountAppliedSearchCriteria;
import shopping.international.domain.service.common.ICurrencyConfigService;
import shopping.international.domain.service.orders.IAdminDiscountService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.currency.CurrencyConfig;
import shopping.international.types.enums.ApiCode;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

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
     * 货币配置服务
     */
    private final ICurrencyConfigService currencyConfigService;

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
        List<DiscountPolicyRespond> data = pageData.items().stream().map(this::toRespond).toList();
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

        BigDecimal percentOff = parseBigDecimalOrNull(req.getPercentOff(), "percentOff");
        List<DiscountPolicyAmount> amounts = toPolicyAmounts(req.getAmounts());

        DiscountPolicy created = adminDiscountService.createPolicy(
                DiscountPolicy.create(
                        req.getName(),
                        req.getApplyScope(),
                        req.getStrategyType(),
                        percentOff,
                        amounts
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

        BigDecimal percentOff = parseBigDecimalOrNull(req.getPercentOff(), "percentOff");
        List<DiscountPolicyAmount> amounts = toPolicyAmounts(req.getAmounts());

        DiscountPolicy toUpdate = DiscountPolicy.create(
                req.getName(),
                req.getApplyScope(),
                req.getStrategyType(),
                percentOff,
                amounts
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

        DiscountCode toUpdate = DiscountCode.create(
                DiscountCodeText.ofNullable(req.getCode()),
                req.getPolicyId(),
                req.getName(),
                req.getScopeMode(),
                req.getExpiresAt()
        );
        DiscountCode updated = adminDiscountService.updateCode(codeId, toUpdate);
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
        List<DiscountAppliedViewRespond> data = pageData.items().stream()
                .map(v -> {
                    CurrencyConfig currencyConfig = v.currency() == null ? null : currencyConfigService.get(v.currency());
                    return DiscountAppliedViewRespond.builder()
                            .orderNo(v.orderNo())
                            .orderItemId(v.orderItemId())
                            .discountCodeId(v.discountCodeId())
                            .appliedScope(v.appliedScope())
                            .appliedAmount(currencyConfig == null || v.appliedAmountMinor() == null
                                    ? null
                                    : currencyConfig.toMajor(v.appliedAmountMinor()).toPlainString())
                            .createdAt(v.createdAt())
                            .build();
                })
                .toList();
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
    private DiscountPolicyRespond toRespond(DiscountPolicy policy) {
        List<DiscountPolicyRespond.DiscountPolicyAmountRespond> amounts = policy.getAmounts().stream()
        .filter(Objects::nonNull)
        .map(a -> {
            CurrencyConfig currencyConfig = currencyConfigService.get(a.getCurrency());
            return DiscountPolicyRespond.DiscountPolicyAmountRespond.builder()
                    .currency(a.getCurrency())
                    .amountOff(a.getAmountOffMinor() == null ? null : currencyConfig.toMajor(a.getAmountOffMinor()).toPlainString())
                    .minOrderAmount(a.getMinOrderAmountMinor() == null ? null : currencyConfig.toMajor(a.getMinOrderAmountMinor()).toPlainString())
                    .maxDiscountAmount(a.getMaxDiscountAmountMinor() == null ? null : currencyConfig.toMajor(a.getMaxDiscountAmountMinor()).toPlainString())
                    .build();
        })
        .toList();
        return DiscountPolicyRespond.builder()
                .id(policy.getId())
                .name(policy.getName())
                .applyScope(policy.getApplyScope())
                .strategyType(policy.getStrategyType())
                .percentOff(policy.getPercentOff() == null ? null : policy.getPercentOff().doubleValue())
                .amounts(amounts)
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }

    /**
     * 将请求体的金额配置列表转换为领域实体列表
     *
     * @param amountReqs 请求体金额配置列表 (可为空)
     * @return 领域实体列表
     */
    private List<DiscountPolicyAmount> toPolicyAmounts(@Nullable List<DiscountPolicyUpsertRequest.DiscountPolicyAmountUpsertRequest> amountReqs) {
        if (amountReqs == null || amountReqs.isEmpty())
            return List.of();
        return amountReqs.stream()
                .filter(Objects::nonNull)
                .map(a -> {
                    CurrencyConfig currencyConfig = currencyConfigService.get(a.getCurrency());
                    Long amountOffMinor = toMinorOrNull(currencyConfig, a.getAmountOff(), "amountOff", true);
                    Long minOrderAmountMinor = toMinorOrNull(currencyConfig, a.getMinOrderAmount(), "minOrderAmount", false);
                    Long maxDiscountAmountMinor = toMinorOrNull(currencyConfig, a.getMaxDiscountAmount(), "maxDiscountAmount", false);
                    return DiscountPolicyAmount.of(a.getCurrency(), amountOffMinor, minOrderAmountMinor, maxDiscountAmountMinor);
                })
                .toList();
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
     * 将给定的字符串转换为 <code>BigDecimal</code> 对象 如果字符串为空或空白 则返回 null
     *
     * @param raw       输入的原始字符串 可以为 null
     * @param fieldName 字段名称 用于在抛出异常时提供上下文信息
     * @return 如果输入字符串可以被解析为 <code>BigDecimal</code> 则返回对应的 <code>BigDecimal</code> 对象 否则返回 null 或者当解析失败时抛出异常
     * @throws IllegalParamException 当输入字符串无法被解析为有效的 <code>BigDecimal</code> 时抛出此异常 包含字段名以指示哪个字段值不合法
     */
    private static @Nullable BigDecimal parseBigDecimalOrNull(@Nullable String raw, String fieldName) {
        if (raw == null || raw.isBlank())
            return null;
        String trimmed = raw.strip();
        try {
            return new BigDecimal(trimmed);
        } catch (Exception e) {
            throw new IllegalParamException(fieldName + " 数值不合法");
        }
    }

    /**
     * 将给定的字符串转换为货币配置中的最小单位, 如果无法转换则返回 null
     *
     * @param currencyConfig  货币配置对象, 用于进行货币单位之间的换算, 如果为 null 则会抛出异常
     * @param raw             原始字符串, 代表需要转换的数值, 如果为空或仅包含空白字符, 方法将直接返回 null
     * @param fieldName       字段名称, 主要用于在抛出异常时提供上下文信息
     * @param requirePositive 指示转换后的数值是否必须为正数, 如果设置为 true 且转换结果不大于 0, 或者设置为 false 但转换结果小于 0, 都将抛出异常
     * @return 返回转换后的 long 类型值, 表示原始数值在指定货币配置下的最小单位表示, 如果无法转换则返回 null
     * @throws IllegalParamException 当 currencyConfig 为 null, 或者根据 requirePositive 参数检查失败时抛出
     */
    private static @Nullable Long toMinorOrNull(@Nullable CurrencyConfig currencyConfig,
                                                @Nullable String raw,
                                                String fieldName,
                                                boolean requirePositive) {
        if (raw == null || raw.isBlank())
            return null;
        if (currencyConfig == null)
            throw new IllegalParamException(fieldName + " 需要提供 currency 才能进行换算");
        BigDecimal major = parseBigDecimalOrNull(raw, fieldName);
        if (major == null)
            return null;
        long minor = currencyConfig.toMinorRounded(major);
        if (requirePositive && minor <= 0)
            throw new IllegalParamException(fieldName + " 必须大于 0");
        if (!requirePositive && minor < 0)
            throw new IllegalParamException(fieldName + " 不能为负数");
        return minor;
    }
}
