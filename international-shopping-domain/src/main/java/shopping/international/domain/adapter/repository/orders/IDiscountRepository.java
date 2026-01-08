package shopping.international.domain.adapter.repository.orders;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.aggregate.orders.DiscountCode;
import shopping.international.domain.model.aggregate.orders.DiscountPolicy;
import shopping.international.domain.model.vo.orders.DiscountCodeSearchCriteria;
import shopping.international.domain.model.vo.orders.DiscountCodeText;
import shopping.international.domain.model.vo.orders.DiscountPolicySearchCriteria;
import shopping.international.domain.model.vo.orders.OrderDiscountAppliedSearchCriteria;
import shopping.international.domain.service.orders.IAdminDiscountService;

import java.util.List;
import java.util.Optional;

/**
 * 折扣相关聚合仓储接口 (面向订单域)
 *
 * <p>覆盖:</p>
 * <ul>
 *     <li>{@code discount_policy} 折扣策略</li>
 *     <li>{@code discount_code} 折扣码</li>
 *     <li>{@code discount_code_product} 折扣码适用商品映射</li>
 *     <li>{@code order_discount_applied} 折扣实际使用流水 (查询)</li>
 * </ul>
 */
public interface IDiscountRepository {

    // ========================= discount_policy =========================

    /**
     * 分页查询折扣策略列表
     *
     * @param criteria 查询条件
     * @param offset   偏移量, 从 0 开始
     * @param limit    单页数量
     * @return 策略列表
     */
    @NotNull
    List<DiscountPolicy> pagePolicies(@NotNull DiscountPolicySearchCriteria criteria, int offset, int limit);

    /**
     * 统计折扣策略数量
     *
     * @param criteria 查询条件
     * @return 总数
     */
    long countPolicies(@NotNull DiscountPolicySearchCriteria criteria);

    /**
     * 按主键查询折扣策略
     *
     * @param policyId 策略 ID
     * @return 若存在返回 Optional
     */
    @NotNull
    Optional<DiscountPolicy> findPolicyById(@NotNull Long policyId);

    /**
     * 根据名称查找折扣策略
     *
     * @param name 折扣策略的名称, 不能为 null
     * @return 如果找到了指定名称的折扣策略, 则返回包含该策略的 Optional; 否则, 返回空的 Optional
     */
    @NotNull
    Optional<DiscountPolicy> findPolicyByName(@NotNull String name);

    /**
     * 保存新折扣策略
     *
     * @param policy 待保存策略 (id 为空)
     * @return 保存后的策略快照
     */
    @NotNull
    DiscountPolicy savePolicy(@NotNull DiscountPolicy policy);

    /**
     * 更新折扣策略
     *
     * @param policy 待更新策略 (id 不为空)
     * @return 更新后的策略快照
     */
    @NotNull
    DiscountPolicy updatePolicy(@NotNull DiscountPolicy policy);

    /**
     * 删除折扣策略
     *
     * @param policyId 策略 ID
     */
    void deletePolicy(@NotNull Long policyId);

    // ========================= discount_code =========================

    /**
     * 分页查询折扣码列表
     *
     * @param criteria 查询条件
     * @param offset   偏移量, 从 0 开始
     * @param limit    单页数量
     * @return 折扣码列表
     */
    @NotNull
    List<DiscountCode> pageCodes(@NotNull DiscountCodeSearchCriteria criteria, int offset, int limit);

    /**
     * 统计折扣码数量
     *
     * @param criteria 查询条件
     * @return 总数
     */
    long countCodes(@NotNull DiscountCodeSearchCriteria criteria);

    /**
     * 按主键查询折扣码
     *
     * @param codeId 折扣码 ID
     * @return 若存在返回 Optional
     */
    @NotNull
    Optional<DiscountCode> findCodeById(@NotNull Long codeId);

    /**
     * 按折扣码文本查询折扣码
     *
     * @param code 折扣码文本值对象
     * @return 若存在返回 Optional
     */
    @NotNull
    Optional<DiscountCode> findCodeByText(@NotNull DiscountCodeText code);

    /**
     * 根据给定的策略 ID 统计关联的折扣码数量
     *
     * @param policyId 策略 ID
     * @return 与该策略关联的折扣码总数
     */
    @NotNull
    Long countCodeByPolicyId(@NotNull Long policyId);

    /**
     * 保存新折扣码
     *
     * @param code 待保存折扣码 (id 为空)
     * @return 保存后的折扣码快照
     */
    @NotNull
    DiscountCode saveCode(@NotNull DiscountCode code);

    /**
     * 更新折扣码
     *
     * @param code 待更新折扣码 (id 不为空)
     * @return 更新后的折扣码快照
     */
    @NotNull
    DiscountCode updateCode(@NotNull DiscountCode code);

    /**
     * 删除折扣码 (同时清理映射)
     *
     * @param codeId 折扣码 ID
     */
    void deleteCode(@NotNull Long codeId);

    // ========================= discount_code_product =========================

    /**
     * 获取折扣码适用商品映射 (SPU ID 列表)
     *
     * @param codeId 折扣码 ID
     * @return SPU ID 列表
     */
    @NotNull
    List<Long> listCodeProductIds(@NotNull Long codeId);

    /**
     * 覆盖设置折扣码适用商品映射
     *
     * @param codeId     折扣码 ID
     * @param productIds SPU ID 列表
     * @return 生效后的 SPU ID 列表
     */
    @NotNull
    List<Long> replaceCodeProducts(@NotNull Long codeId, @NotNull List<Long> productIds);

    // ========================= order_discount_applied (query) =========================

    /**
     * 分页查询折扣实际使用流水
     *
     * @param criteria 查询条件
     * @param offset   偏移量, 从 0 开始
     * @param limit    单页数量
     * @return 流水列表
     */
    @NotNull
    List<IAdminDiscountService.OrderDiscountAppliedView> pageOrderDiscountApplied(@NotNull OrderDiscountAppliedSearchCriteria criteria, int offset, int limit);

    /**
     * 统计折扣实际使用流水数量
     *
     * @param criteria 查询条件
     * @return 总数
     */
    long countOrderDiscountApplied(@NotNull OrderDiscountAppliedSearchCriteria criteria);
}
