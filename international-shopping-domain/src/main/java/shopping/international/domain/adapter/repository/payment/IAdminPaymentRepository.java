package shopping.international.domain.adapter.repository.payment;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.payment.*;

import java.util.Optional;

/**
 * 管理侧支付读模型仓储接口 (Repository)
 *
 * <p>说明：该接口仅承载后台/排障/对账的只读查询能力</p>
 */
public interface IAdminPaymentRepository {

    /**
     * 分页查询管理侧支付单列表
     *
     * @param criteria  查询条件 包含订单 ID, 订单号, 支付渠道等信息
     * @param pageQuery 分页参数 指定了请求的页码和每页的数据量
     * @return 分页结果 包含当前页的支付单列表数据以及满足条件的支付单总数
     */
    @NotNull PageResult<AdminPaymentListItemView> pagePayments(@NotNull AdminPaymentSearchCriteria criteria, @NotNull PageQuery pageQuery);

    /**
     * 根据支付单 ID 获取管理侧支付单详情
     *
     * @param paymentId 支付单 ID 必须提供
     * @return 返回一个包含 {@link AdminPaymentDetail} 的 {@link Optional} 对象, 如果没有找到对应的支付单则返回空的 {@link Optional}
     */
    @NotNull Optional<AdminPaymentDetail> getPaymentDetail(@NotNull Long paymentId);

    /**
     * 分页查询管理侧退款单列表
     *
     * @param criteria  查询条件 包含订单 ID, 订单号, 支付渠道等信息
     * @param pageQuery 分页参数 指定了请求的页码和每页的数据量
     * @return 分页结果 包含当前页的退款单列表数据以及满足条件的退款单总数
     */
    @NotNull PageResult<AdminRefundListItemView> pageRefunds(@NotNull AdminRefundSearchCriteria criteria, @NotNull PageQuery pageQuery);

    /**
     * 根据退款单 ID 获取管理侧退款单详情
     *
     * @param refundId 退款单 ID 必须提供
     * @return 返回一个包含 {@link AdminRefundDetail} 的 {@link Optional} 对象, 如果没有找到对应的退款单则返回空的 {@link Optional}
     */
    @NotNull Optional<AdminRefundDetail> getRefundDetail(@NotNull Long refundId);
}

