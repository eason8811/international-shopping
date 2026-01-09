package shopping.international.domain.service.orders;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.orders.DiscountCode;
import shopping.international.domain.model.aggregate.orders.DiscountPolicy;
import shopping.international.domain.model.enums.orders.DiscountApplyScope;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.orders.DiscountCodeSearchCriteria;
import shopping.international.domain.model.vo.orders.DiscountPolicySearchCriteria;
import shopping.international.domain.model.vo.orders.OrderDiscountAppliedSearchCriteria;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理侧折扣管理领域服务接口
 *
 * <p>覆盖:</p>
 * <ul>
 *     <li>折扣策略 CRUD</li>
 *     <li>折扣码 CRUD</li>
 *     <li>折扣码适用商品映射覆盖</li>
 *     <li>折扣实际使用流水查询</li>
 * </ul>
 */
public interface IAdminDiscountService {
    /**
     * 折扣实际使用流水展示项
     *
     * @param id                 主键 ID
     * @param orderNo            订单号
     * @param orderId            订单 ID
     * @param orderItemId        订单明细 ID (可为空)
     * @param discountCodeId     折扣码 ID
     * @param appliedScope       应用范围
     * @param currency           订单币种 (可为空)
     * @param appliedAmountMinor 实际抵扣金额 (最小货币单位)
     * @param createdAt          发生时间
     */
    record OrderDiscountAppliedView(Long id,
                                    String orderNo,
                                    Long orderId,
                                    @Nullable Long orderItemId,
                                    Long discountCodeId,
                                    DiscountApplyScope appliedScope,
                                    @Nullable String currency,
                                    Long appliedAmountMinor,
                                    LocalDateTime createdAt) {
    }

    /**
     * 查询折扣策略列表
     *
     * @param pageQuery 分页参数
     * @param criteria  筛选条件
     * @return 分页结果
     */
    @NotNull
    PageResult<DiscountPolicy> listPolicies(@NotNull PageQuery pageQuery, @NotNull DiscountPolicySearchCriteria criteria);

    /**
     * 创建折扣策略
     *
     * @param policy 新策略
     * @return 保存后的策略
     */
    @NotNull
    DiscountPolicy createPolicy(@NotNull DiscountPolicy policy);

    /**
     * 更新折扣策略
     *
     * @param policyId 策略 ID
     * @param toUpdate 用于更新的 Policy 对象
     * @return 更新后的策略
     */
    @NotNull
    DiscountPolicy updatePolicy(@NotNull Long policyId, @NotNull DiscountPolicy toUpdate);

    /**
     * 重算指定折扣策略(AMOUNT)的 FX_AUTO / 缺失币种金额配置
     *
     * <p>不会覆盖已有 MANUAL 币种；基于 USD 基准金额 + 最新汇率派生其余币种</p>
     *
     * @param policyId 策略 ID
     * @return 受影响的币种列表
     */
    @NotNull
    List<String> recomputeFxAmounts(@NotNull Long policyId);

    /**
     * 全量重算所有折扣策略(AMOUNT)的 FX_AUTO / 缺失币种金额配置
     *
     * @param batchSize 每批处理的策略数量
     * @return 处理的策略数量
     */
    int recomputeFxAmountsAll(int batchSize);

    /**
     * 将指定折扣策略(AMOUNT)的金额配置模式切换为 MANUAL (冻结金额, 清空 FX 元数据)
     *
     * @param policyId 策略 ID
     * @param currency 币种
     * @return 受影响的币种
     */
    @NotNull
    String switchPolicyAmountsToManual(@NotNull Long policyId, @NotNull String currency);

    /**
     * 将指定折扣策略(AMOUNT)的金额配置模式切换为 FX_AUTO (除 USD 外全部按汇率派生)
     *
     * @param policyId 策略 ID
     * @param currency 币种
     * @return 受影响的币种
     */
    @NotNull
    String switchPolicyAmountsToFxAuto(@NotNull Long policyId, @NotNull String currency);

    /**
     * 删除折扣策略
     *
     * @param policyId 策略 ID
     */
    void deletePolicy(@NotNull Long policyId);

    /**
     * 查询折扣码列表
     *
     * @param pageQuery 分页参数
     * @param criteria  筛选条件
     * @return 分页结果
     */
    @NotNull
    PageResult<DiscountCode> listCodes(@NotNull PageQuery pageQuery, @NotNull DiscountCodeSearchCriteria criteria);

    /**
     * 创建折扣码
     *
     * @param code 新折扣码聚合
     * @return 保存后的折扣码
     */
    @NotNull
    DiscountCode createCode(@NotNull DiscountCode code);

    /**
     * 更新折扣码
     *
     * @param codeId   折扣码 ID
     * @param toUpdate 用于跟新的 Code 对象
     * @return 更新后的折扣码
     */
    @NotNull
    DiscountCode updateCode(@NotNull Long codeId, @NotNull DiscountCode toUpdate);

    /**
     * 删除折扣码
     *
     * @param codeId 折扣码 ID
     */
    void deleteCode(@NotNull Long codeId);

    /**
     * 获取折扣码适用商品映射 (SPU 列表)
     *
     * @param codeId 折扣码 ID
     * @return SPU ID 列表
     */
    @NotNull
    List<Long> listCodeProducts(@NotNull Long codeId);

    /**
     * 覆盖设置折扣码适用商品映射 (SPU 列表)
     *
     * @param codeId     折扣码 ID
     * @param productIds SPU ID 列表
     * @return 生效后的 SPU ID 列表
     */
    @NotNull
    List<Long> replaceCodeProducts(@NotNull Long codeId, @NotNull List<Long> productIds);

    /**
     * 查询折扣实际使用流水
     *
     * @param pageQuery 分页参数
     * @param criteria  筛选条件
     * @return 分页结果
     */
    @NotNull
    PageResult<OrderDiscountAppliedView> listOrderDiscountApplied(@NotNull PageQuery pageQuery, @NotNull OrderDiscountAppliedSearchCriteria criteria);
}
