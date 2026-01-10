package shopping.international.infrastructure.adapter.repository.orders;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.repository.orders.IDiscountRepository;
import shopping.international.domain.model.aggregate.orders.DiscountCode;
import shopping.international.domain.model.aggregate.orders.DiscountPolicy;
import shopping.international.domain.model.entity.orders.DiscountPolicyAmount;
import shopping.international.domain.model.enums.orders.DiscountApplyScope;
import shopping.international.domain.model.enums.orders.DiscountPolicyAmountSource;
import shopping.international.domain.model.enums.orders.DiscountScopeMode;
import shopping.international.domain.model.enums.orders.DiscountStrategyType;
import shopping.international.domain.model.vo.orders.DiscountCodeSearchCriteria;
import shopping.international.domain.model.vo.orders.DiscountCodeText;
import shopping.international.domain.model.vo.orders.DiscountPolicySearchCriteria;
import shopping.international.domain.model.vo.orders.OrderDiscountAppliedSearchCriteria;
import shopping.international.domain.service.orders.IAdminDiscountService;
import shopping.international.infrastructure.dao.orders.*;
import shopping.international.infrastructure.dao.orders.po.*;
import shopping.international.types.enums.FxRateProvider;
import shopping.international.types.exceptions.ConflictException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 基于 MyBatis-Plus 的折扣仓储实现
 */
@Repository
@RequiredArgsConstructor
public class DiscountRepository implements IDiscountRepository {

    /**
     * 折扣策略 Mapper
     */
    private final DiscountPolicyMapper discountPolicyMapper;
    /**
     * 折扣策略币种金额配置 Mapper
     */
    private final DiscountPolicyAmountMapper discountPolicyAmountMapper;
    /**
     * 折扣码 Mapper
     */
    private final DiscountCodeMapper discountCodeMapper;
    /**
     * 折扣码-商品映射 Mapper
     */
    private final DiscountCodeProductMapper discountCodeProductMapper;
    /**
     * 折扣实际使用流水 Mapper
     */
    private final OrderDiscountAppliedMapper orderDiscountAppliedMapper;

    /**
     * 分页查询折扣策略
     *
     * @param criteria 筛选条件
     * @param offset   偏移量
     * @param limit    单页数量
     * @return 策略列表
     */
    @Override
    public @NotNull List<DiscountPolicy> pagePolicies(@NotNull DiscountPolicySearchCriteria criteria, int offset, int limit) {
        List<DiscountPolicyPO> pos = discountPolicyMapper.pageWithAmounts(
                criteria.getName(),
                criteria.getApplyScope() == null ? null : criteria.getApplyScope().name(),
                criteria.getStrategyType() == null ? null : criteria.getStrategyType().name(),
                offset,
                limit
        );
        if (pos == null || pos.isEmpty())
            return List.of();
        return pos.stream().map(po -> toAggregate(po, po.getAmounts())).toList();
    }

    /**
     * 统计折扣策略数量
     *
     * @param criteria 筛选条件
     * @return 总数
     */
    @Override
    public long countPolicies(@NotNull DiscountPolicySearchCriteria criteria) {
        LambdaQueryWrapper<DiscountPolicyPO> wrapper = buildPagePoliciesWrapper(criteria);
        return discountPolicyMapper.selectCount(wrapper);
    }

    /**
     * 按主键查询折扣策略
     *
     * @param policyId 策略 ID
     * @return Optional
     */
    @Override
    public @NotNull Optional<DiscountPolicy> findPolicyById(@NotNull Long policyId) {
        DiscountPolicyPO po = discountPolicyMapper.selectById(policyId);
        if (po == null)
            return Optional.empty();
        List<DiscountPolicyAmountPO> amountPos = discountPolicyAmountMapper.selectList(new LambdaQueryWrapper<DiscountPolicyAmountPO>()
                .eq(DiscountPolicyAmountPO::getPolicyId, policyId));
        return Optional.of(toAggregate(po, amountPos));
    }

    /**
     * 根据名称查找折扣策略
     *
     * @param name 折扣策略的名称, 不能为 null
     * @return 如果找到了指定名称的折扣策略, 则返回包含该策略的 Optional; 否则, 返回空的 Optional
     */
    @Override
    public @NotNull Optional<DiscountPolicy> findPolicyByName(@NotNull String name) {
        DiscountPolicyPO discountPolicyPO = discountPolicyMapper.selectOne(new LambdaQueryWrapper<DiscountPolicyPO>()
                .eq(DiscountPolicyPO::getName, name)
                .last("limit 1"));
        if (discountPolicyPO == null)
            return Optional.empty();
        List<DiscountPolicyAmountPO> amountPos = discountPolicyAmountMapper.selectList(new LambdaQueryWrapper<DiscountPolicyAmountPO>()
                .eq(DiscountPolicyAmountPO::getPolicyId, discountPolicyPO.getId()));
        return Optional.of(toAggregate(discountPolicyPO, amountPos));
    }

    /**
     * 保存新策略
     *
     * @param policy 新策略
     * @return 保存后的策略
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull DiscountPolicy savePolicy(@NotNull DiscountPolicy policy) {
        DiscountPolicyPO po = DiscountPolicyPO.builder()
                .name(policy.getName())
                .applyScope(policy.getApplyScope().name())
                .strategyType(policy.getStrategyType().name())
                .percentOff(policy.getPercentOff())
                .build();
        discountPolicyMapper.insert(po);
        replacePolicyAmounts(po.getId(), policy.getAmounts());
        return findPolicyById(po.getId()).orElseThrow(() -> new ConflictException("折扣策略创建后回读失败"));
    }

    /**
     * 更新策略
     *
     * @param policy 策略
     * @return 更新后的策略
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull DiscountPolicy updatePolicy(@NotNull DiscountPolicy policy) {
        LambdaUpdateWrapper<DiscountPolicyPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DiscountPolicyPO::getId, policy.getId())
                .set(DiscountPolicyPO::getName, policy.getName())
                .set(DiscountPolicyPO::getApplyScope, policy.getApplyScope().name())
                .set(DiscountPolicyPO::getStrategyType, policy.getStrategyType().name())
                .set(DiscountPolicyPO::getPercentOff, policy.getPercentOff());
        discountPolicyMapper.update(null, wrapper);
        replacePolicyAmounts(policy.getId(), policy.getAmounts());
        return findPolicyById(policy.getId()).orElseThrow(() -> new ConflictException("折扣策略更新后回读失败"));
    }

    /**
     * 删除折扣策略
     *
     * @param policyId 策略 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePolicy(@NotNull Long policyId) {
        discountPolicyAmountMapper.delete(new LambdaQueryWrapper<DiscountPolicyAmountPO>()
                .eq(DiscountPolicyAmountPO::getPolicyId, policyId));
        discountPolicyMapper.deleteById(policyId);
    }

    /**
     * 覆盖写入折扣策略的币种金额配置
     *
     * <p>该方法会先清理旧配置, 再插入新配置, 需要在事务内调用</p>
     *
     * @param policyId 策略 ID
     * @param amounts  币种金额配置列表 (可为空)
     */
    private void replacePolicyAmounts(@NotNull Long policyId, @NotNull List<DiscountPolicyAmount> amounts) {
        discountPolicyAmountMapper.delete(new LambdaQueryWrapper<DiscountPolicyAmountPO>()
                .eq(DiscountPolicyAmountPO::getPolicyId, policyId));
        if (amounts.isEmpty())
            return;
        List<DiscountPolicyAmountPO> poList = new ArrayList<>();
        for (DiscountPolicyAmount a : amounts) {
            if (a == null)
                continue;
            poList.add(DiscountPolicyAmountPO.builder()
                    .policyId(policyId)
                    .currency(a.getCurrency())
                    .amountOff(a.getAmountOffMinor())
                    .minOrderAmount(a.getMinOrderAmountMinor())
                    .maxDiscountAmount(a.getMaxDiscountAmountMinor())
                    .amountSource(a.getSource() == null ? DiscountPolicyAmountSource.MANUAL.name() : a.getSource().name())
                    .derivedFrom(a.getDerivedFrom())
                    .fxRate(a.getFxRate())
                    .fxAsOf(a.getFxAsOf())
                    .fxProvider(a.getFxProvider() == null ? null : a.getFxProvider().name())
                    .computedAt(a.getComputedAt())
                    .build());
        }
        if (poList.isEmpty())
            return;
        discountPolicyAmountMapper.insert(poList);
    }

    /**
     * 分页查询折扣码
     *
     * @param criteria 筛选条件
     * @param offset   偏移量
     * @param limit    单页数量
     * @return 折扣码列表
     */
    @Override
    public @NotNull List<DiscountCode> pageCodes(@NotNull DiscountCodeSearchCriteria criteria, int offset, int limit) {
        LambdaQueryWrapper<DiscountCodePO> wrapper = buildPageCodesWrapper(criteria);
        wrapper.orderByDesc(DiscountCodePO::getId)
                .last("limit " + limit + " offset " + offset);
        List<DiscountCodePO> pos = discountCodeMapper.selectList(wrapper);
        if (pos == null || pos.isEmpty())
            return List.of();
        return pos.stream().map(this::toAggregate).toList();
    }

    /**
     * 统计折扣码数量
     *
     * @param criteria 筛选条件
     * @return 总数
     */
    @Override
    public long countCodes(@NotNull DiscountCodeSearchCriteria criteria) {
        LambdaQueryWrapper<DiscountCodePO> wrapper = buildPageCodesWrapper(criteria);
        return discountCodeMapper.selectCount(wrapper);
    }

    /**
     * 按主键查询折扣码
     *
     * @param codeId 折扣码 ID
     * @return Optional
     */
    @Override
    public @NotNull Optional<DiscountCode> findCodeById(@NotNull Long codeId) {
        DiscountCodePO po = discountCodeMapper.selectById(codeId);
        return po == null ? Optional.empty() : Optional.of(toAggregate(po));
    }

    /**
     * 按文本查询折扣码
     *
     * @param code 折扣码文本
     * @return Optional
     */
    @Override
    public @NotNull Optional<DiscountCode> findCodeByText(@NotNull DiscountCodeText code) {
        DiscountCodePO po = discountCodeMapper.selectOne(new LambdaQueryWrapper<DiscountCodePO>()
                .eq(DiscountCodePO::getCode, code.getValue())
                .last("limit 1"));
        return po == null ? Optional.empty() : Optional.of(toAggregate(po));
    }

    /**
     * 统计指定策略下“仍可用”的折扣码数量
     *
     * <p>仍可用的定义:</p>
     * <ul>
     *     <li>{@code permanent=true}</li>
     *     <li>或 {@code expiresAt >= now}</li>
     * </ul>
     *
     * @param policyId 策略 ID
     * @param now      当前时间
     * @return 可用折扣码数量
     */
    @Override
    public @NotNull Long countActiveCodesByPolicyId(@NotNull Long policyId, @NotNull LocalDateTime now) {
        LambdaQueryWrapper<DiscountCodePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DiscountCodePO::getPolicyId, policyId)
                .and(w -> w
                        .eq(DiscountCodePO::getPermanent, true)
                        .or()
                        .isNull(DiscountCodePO::getExpiresAt)
                        .or()
                        .ge(DiscountCodePO::getExpiresAt, now)
                );
        return discountCodeMapper.selectCount(wrapper);
    }

    /**
     * 删除指定策略下已过期的折扣码 (同时清理折扣码-商品映射)
     *
     * <p>已过期的定义: {@code permanent=false} 且 {@code expiresAt < now}</p>
     *
     * @param policyId 策略 ID
     * @param now      当前时间
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteExpiredCodesByPolicyId(@NotNull Long policyId, @NotNull LocalDateTime now) {
        LambdaQueryWrapper<DiscountCodePO> expiredWrapper = new LambdaQueryWrapper<>();
        expiredWrapper.select(DiscountCodePO::getId)
                .eq(DiscountCodePO::getPolicyId, policyId)
                .isNotNull(DiscountCodePO::getExpiresAt)
                .lt(DiscountCodePO::getExpiresAt, now)
                .and(w -> w
                        .eq(DiscountCodePO::getPermanent, false)
                        .or()
                        .isNull(DiscountCodePO::getPermanent)
                );

        List<DiscountCodePO> expiredPos = discountCodeMapper.selectList(expiredWrapper);
        if (expiredPos == null || expiredPos.isEmpty())
            return;

        List<Long> ids = expiredPos.stream().map(DiscountCodePO::getId).filter(Objects::nonNull).toList();
        if (ids.isEmpty())
            return;

        discountCodeProductMapper.delete(new LambdaQueryWrapper<DiscountCodeProductPO>()
                .in(DiscountCodeProductPO::getDiscountCodeId, ids));
        discountCodeMapper.deleteByIds(ids);
    }

    /**
     * 保存新折扣码
     *
     * @param code 折扣码
     * @return 保存后的折扣码
     */
    @Override
    public @NotNull DiscountCode saveCode(@NotNull DiscountCode code) {
        DiscountCodePO po = DiscountCodePO.builder()
                .code(code.getCode().getValue())
                .policyId(code.getPolicyId())
                .name(code.getName())
                .scopeMode(code.getScopeMode().name())
                .expiresAt(code.getExpiresAt())
                .permanent(code.getPermanent())
                .build();
        try {
            discountCodeMapper.insert(po);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("折扣码唯一约束冲突", e);
        }
        return findCodeById(po.getId()).orElseThrow(() -> new ConflictException("折扣码创建后回读失败"));
    }

    /**
     * 更新折扣码
     *
     * @param code 折扣码
     * @return 更新后的折扣码
     */
    @Override
    public @NotNull DiscountCode updateCode(@NotNull DiscountCode code) {
        LambdaUpdateWrapper<DiscountCodePO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DiscountCodePO::getId, code.getId())
                .set(DiscountCodePO::getPolicyId, code.getPolicyId())
                .set(DiscountCodePO::getName, code.getName())
                .set(DiscountCodePO::getScopeMode, code.getScopeMode().name())
                .set(DiscountCodePO::getExpiresAt, code.getExpiresAt())
                .set(DiscountCodePO::getPermanent, code.getPermanent());
        discountCodeMapper.update(null, wrapper);
        return findCodeById(code.getId()).orElseThrow(() -> new ConflictException("折扣码更新后回读失败"));
    }

    /**
     * 删除折扣码 (同时清理映射)
     *
     * @param codeId 折扣码 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCode(@NotNull Long codeId) {
        discountCodeProductMapper.delete(new LambdaQueryWrapper<DiscountCodeProductPO>()
                .eq(DiscountCodeProductPO::getDiscountCodeId, codeId));
        discountCodeMapper.deleteById(codeId);
    }

    /**
     * 获取折扣码适用商品 ID 列表
     *
     * @param codeId 折扣码 ID
     * @return SPU ID 列表
     */
    @Override
    public @NotNull List<Long> listCodeProductIds(@NotNull Long codeId) {
        List<DiscountCodeProductPO> pos = discountCodeProductMapper.selectList(new LambdaQueryWrapper<DiscountCodeProductPO>()
                .eq(DiscountCodeProductPO::getDiscountCodeId, codeId)
                .orderByAsc(DiscountCodeProductPO::getProductId));
        if (pos == null || pos.isEmpty())
            return List.of();
        return pos.stream().map(DiscountCodeProductPO::getProductId).toList();
    }

    /**
     * 覆盖设置折扣码适用商品映射
     *
     * @param codeId     折扣码 ID
     * @param productIds SPU ID 列表
     * @return 生效后的列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull List<Long> replaceCodeProducts(@NotNull Long codeId, @NotNull List<Long> productIds) {
        discountCodeProductMapper.delete(new LambdaQueryWrapper<DiscountCodeProductPO>()
                .eq(DiscountCodeProductPO::getDiscountCodeId, codeId));

        List<DiscountCodeProductPO> toUpdatePOList = productIds.stream()
                .map(id ->
                        DiscountCodeProductPO.builder()
                                .discountCodeId(codeId)
                                .productId(id)
                                .build()
                )
                .toList();
        discountCodeProductMapper.insert(toUpdatePOList);

        return listCodeProductIds(codeId);
    }

    /**
     * 分页查询折扣实际使用流水
     *
     * @param criteria 筛选条件
     * @param offset   偏移量
     * @param limit    单页数量
     * @return 流水列表
     */
    @Override
    public @NotNull List<IAdminDiscountService.OrderDiscountAppliedView> pageOrderDiscountApplied(@NotNull OrderDiscountAppliedSearchCriteria criteria,
                                                                                                  int offset, int limit) {
        List<OrderDiscountAppliedViewPO> pos = orderDiscountAppliedMapper.selectViews(
                criteria.getOrderNo(),
                criteria.getDiscountCodeId(),
                criteria.getAppliedScope() == null ? null : criteria.getAppliedScope().name(),
                criteria.getFrom(),
                criteria.getTo(),
                offset,
                limit
        );
        if (pos == null || pos.isEmpty())
            return List.of();
        return pos.stream().map(po -> new IAdminDiscountService.OrderDiscountAppliedView(
                po.getId(),
                po.getOrderNo(),
                po.getOrderId(),
                po.getOrderItemId(),
                po.getDiscountCodeId(),
                DiscountApplyScope.valueOf(po.getAppliedScope()),
                po.getCurrency(),
                po.getAppliedAmount(),
                po.getCreatedAt()
        )).toList();
    }

    /**
     * 统计折扣实际使用流水数量
     *
     * @param criteria 筛选条件
     * @return 总数
     */
    @Override
    public long countOrderDiscountApplied(@NotNull OrderDiscountAppliedSearchCriteria criteria) {
        return orderDiscountAppliedMapper.countViews(
                criteria.getOrderNo(),
                criteria.getDiscountCodeId(),
                criteria.getAppliedScope() == null ? null : criteria.getAppliedScope().name(),
                criteria.getFrom(),
                criteria.getTo()
        );
    }

    /**
     * 构建用于分页查询折扣策略的 <code>LambdaQueryWrapper</code>
     *
     * @param criteria 折扣策略筛选条件 包含 name, applyScope, strategyType 等属性
     * @return LambdaQueryWrapper<DiscountPolicyPO> 根据给定条件构建的查询包装器 用于后续的数据库查询操作
     */
    private LambdaQueryWrapper<DiscountPolicyPO> buildPagePoliciesWrapper(@NotNull DiscountPolicySearchCriteria criteria) {
        LambdaQueryWrapper<DiscountPolicyPO> wrapper = new LambdaQueryWrapper<>();
        if (criteria.getName() != null && !criteria.getName().isBlank())
            wrapper.like(DiscountPolicyPO::getName, criteria.getName());
        if (criteria.getApplyScope() != null)
            wrapper.eq(DiscountPolicyPO::getApplyScope, criteria.getApplyScope().name());
        if (criteria.getStrategyType() != null)
            wrapper.eq(DiscountPolicyPO::getStrategyType, criteria.getStrategyType().name());
        return wrapper;
    }

    /**
     * 构建用于分页查询折扣码的 <code>LambdaQueryWrapper</code>
     *
     * @param criteria 折扣码筛选条件 包含 keyword, policyId, scopeMode, expiresFrom, expiresTo 等属性
     * @return LambdaQueryWrapper<DiscountCodePO> 根据给定条件构建的查询包装器 用于后续的数据库查询操作
     */
    private LambdaQueryWrapper<DiscountCodePO> buildPageCodesWrapper(@NotNull DiscountCodeSearchCriteria criteria) {
        LambdaQueryWrapper<DiscountCodePO> wrapper = new LambdaQueryWrapper<>();
        if (criteria.getPolicyId() != null)
            wrapper.eq(DiscountCodePO::getPolicyId, criteria.getPolicyId());
        if (criteria.getScopeMode() != null)
            wrapper.eq(DiscountCodePO::getScopeMode, criteria.getScopeMode().name());
        if (criteria.getPermanent() != null)
            wrapper.eq(DiscountCodePO::getPermanent, criteria.getPermanent());
        if (criteria.getExpiresFrom() != null)
            wrapper.ge(DiscountCodePO::getExpiresAt, criteria.getExpiresFrom());
        if (criteria.getExpiresTo() != null)
            wrapper.le(DiscountCodePO::getExpiresAt, criteria.getExpiresTo());
        if (criteria.getPermanent() != null)
            wrapper.eq(DiscountCodePO::getPermanent, criteria.getPermanent());
        if (criteria.getKeyword() != null)
            wrapper.and(w -> w
                    .like(DiscountCodePO::getCode, criteria.getKeyword())
                    .or()
                    .like(DiscountCodePO::getName, criteria.getKeyword())
            );
        return wrapper;
    }

    /**
     * PO → DiscountPolicy
     *
     * @param po 持久化对象
     * @return 聚合
     */
    private DiscountPolicy toAggregate(DiscountPolicyPO po, List<DiscountPolicyAmountPO> amountPos) {
        List<DiscountPolicyAmount> amounts = amountPos == null || amountPos.isEmpty()
                ? List.of()
                : amountPos.stream()
                .map(a -> {
                    DiscountPolicyAmountSource source = a.getAmountSource() == null
                            ? DiscountPolicyAmountSource.MANUAL
                            : DiscountPolicyAmountSource.valueOf(a.getAmountSource());
                    FxRateProvider provider = a.getFxProvider() == null ? null : FxRateProvider.valueOf(a.getFxProvider());
                    return DiscountPolicyAmount.reconstitute(
                            a.getCurrency(),
                            a.getAmountOff(),
                            a.getMinOrderAmount(),
                            a.getMaxDiscountAmount(),
                            source,
                            a.getDerivedFrom(),
                            a.getFxRate(),
                            a.getFxAsOf(),
                            provider,
                            a.getComputedAt()
                    );
                })
                .toList();
        return DiscountPolicy.reconstitute(
                po.getId(),
                po.getName(),
                DiscountApplyScope.valueOf(po.getApplyScope()),
                DiscountStrategyType.valueOf(po.getStrategyType()),
                po.getPercentOff(),
                amounts,
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }

    /**
     * PO → DiscountCode
     *
     * @param po 持久化对象
     * @return 聚合
     */
    private DiscountCode toAggregate(DiscountCodePO po) {
        DiscountCodeText codeText = DiscountCodeText.ofNullable(po.getCode());
        if (codeText == null)
            throw new ConflictException("折扣码数据不合法");
        LocalDateTime expiresAt = po.getExpiresAt();
        Boolean permanent = po.getPermanent();
        // 兼容历史数据: 当 permanent 为空时, 以 "expiresAt 是否为空" 推断其值
        if (permanent == null)
            permanent = expiresAt == null;
        // 兜底: permanent=true 时强制忽略 expiresAt
        if (permanent)
            expiresAt = null;
        return DiscountCode.reconstitute(
                po.getId(),
                codeText,
                po.getPolicyId(),
                po.getName(),
                DiscountScopeMode.valueOf(po.getScopeMode()),
                expiresAt,
                permanent,
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }
}
