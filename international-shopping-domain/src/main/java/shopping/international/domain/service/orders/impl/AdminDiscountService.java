package shopping.international.domain.service.orders.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.orders.IDiscountRepository;
import shopping.international.domain.model.aggregate.orders.DiscountCode;
import shopping.international.domain.model.aggregate.orders.DiscountPolicy;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.orders.DiscountCodeSearchCriteria;
import shopping.international.domain.model.vo.orders.DiscountPolicySearchCriteria;
import shopping.international.domain.model.vo.orders.OrderDiscountAppliedSearchCriteria;
import shopping.international.domain.service.orders.IAdminDiscountService;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.List;
import java.util.function.Consumer;

/**
 * 管理侧折扣管理领域服务默认实现
 */
@Service
@RequiredArgsConstructor
public class AdminDiscountService implements IAdminDiscountService {

    /**
     * 折扣仓储
     */
    private final IDiscountRepository discountRepository;

    /**
     * 查询折扣策略列表
     *
     * @param pageQuery 分页条件
     * @param criteria  筛选条件
     * @return 分页结果
     */
    @Override
    public @NotNull PageResult<DiscountPolicy> listPolicies(@NotNull PageQuery pageQuery, @NotNull DiscountPolicySearchCriteria criteria) {
        pageQuery.validate();
        criteria.validate();
        int offset = pageQuery.offset();
        int limit = pageQuery.limit();
        List<DiscountPolicy> items = discountRepository.pagePolicies(criteria, offset, limit);
        long total = discountRepository.countPolicies(criteria);
        return new PageResult<>(items, total);
    }

    /**
     * 创建折扣策略
     *
     * @param policy 新策略
     * @return 保存后的策略
     */
    @Override
    public @NotNull DiscountPolicy createPolicy(@NotNull DiscountPolicy policy) {
        return discountRepository.savePolicy(policy);
    }

    /**
     * 更新折扣策略
     *
     * @param policyId 策略 ID
     * @param toUpdate  用于更新的 Policy 对象
     * @return 更新后的策略
     */
    @Override
    public @NotNull DiscountPolicy updatePolicy(@NotNull Long policyId, @NotNull DiscountPolicy toUpdate) {
        DiscountPolicy policy = discountRepository.findPolicyById(policyId)
                .orElseThrow(() -> new IllegalParamException("折扣策略不存在"));
        policy.update(
                toUpdate.getName(),
                toUpdate.getApplyScope(),
                toUpdate.getStrategyType(),
                toUpdate.getPercentOff(),
                toUpdate.getAmountOff(),
                toUpdate.getCurrency(),
                toUpdate.getMinOrderAmount(),
                toUpdate.getMaxDiscountAmount()
        );
        return discountRepository.updatePolicy(policy);
    }

    /**
     * 删除折扣策略
     *
     * @param policyId 策略 ID
     */
    @Override
    public void deletePolicy(@NotNull Long policyId) {
        if (discountRepository.countCodeByPolicyId(policyId) > 0)
            throw new ConflictException("折扣策略正被折扣码使用, 无法删除");
        discountRepository.deletePolicy(policyId);
    }

    /**
     * 查询折扣码列表
     *
     * @param pageQuery 分页查询条件
     * @param criteria  筛选条件
     * @return 分页结果
     */
    @Override
    public @NotNull PageResult<DiscountCode> listCodes(@NotNull PageQuery pageQuery, @NotNull DiscountCodeSearchCriteria criteria) {
        pageQuery.validate();
        criteria.validate();
        int offset = pageQuery.offset();
        int limit = pageQuery.limit();
        List<DiscountCode> items = discountRepository.pageCodes(criteria, offset, limit);
        long total = discountRepository.countCodes(criteria);
        return new PageResult<>(items, total);
    }

    /**
     * 创建折扣码
     *
     * @param code 新折扣码
     * @return 保存后的折扣码
     */
    @Override
    public @NotNull DiscountCode createCode(@NotNull DiscountCode code) {
        return discountRepository.saveCode(code);
    }

    /**
     * 更新折扣码
     *
     * @param codeId  折扣码 ID
     * @param updater 更新回调
     * @return 更新后的折扣码
     */
    @Override
    public @NotNull DiscountCode updateCode(@NotNull Long codeId, @NotNull Consumer<DiscountCode> updater) {
        DiscountCode code = discountRepository.findCodeById(codeId)
                .orElseThrow(() -> new IllegalParamException("折扣码不存在"));
        updater.accept(code);
        return discountRepository.updateCode(code);
    }

    /**
     * 删除折扣码
     *
     * @param codeId 折扣码 ID
     */
    @Override
    public void deleteCode(@NotNull Long codeId) {
        discountRepository.deleteCode(codeId);
    }

    /**
     * 获取折扣码适用商品映射
     *
     * @param codeId 折扣码 ID
     * @return SPU ID 列表
     */
    @Override
    public @NotNull List<Long> listCodeProducts(@NotNull Long codeId) {
        return discountRepository.listCodeProductIds(codeId);
    }

    /**
     * 覆盖设置折扣码适用商品映射
     *
     * @param codeId     折扣码 ID
     * @param productIds SPU ID 列表
     * @return 生效后的 SPU ID 列表
     */
    @Override
    public @NotNull List<Long> replaceCodeProducts(@NotNull Long codeId, @NotNull List<Long> productIds) {
        return discountRepository.replaceCodeProducts(codeId, productIds);
    }

    /**
     * 查询折扣实际使用流水
     *
     * @param pageQuery 分页查询条件
     * @param criteria  筛选条件
     * @return 分页结果
     */
    @Override
    public @NotNull PageResult<OrderDiscountAppliedView> listOrderDiscountApplied(@NotNull PageQuery pageQuery,
                                                                                  @NotNull OrderDiscountAppliedSearchCriteria criteria) {
        pageQuery.validate();
        criteria.validate();
        int offset = pageQuery.offset();
        int limit = pageQuery.limit();
        List<OrderDiscountAppliedView> items = discountRepository.pageOrderDiscountApplied(criteria, offset, limit);
        long total = discountRepository.countOrderDiscountApplied(criteria);
        return new PageResult<>(items, total);
    }
}
