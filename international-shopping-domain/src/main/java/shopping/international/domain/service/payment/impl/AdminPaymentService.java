package shopping.international.domain.service.payment.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.payment.IAdminPaymentRepository;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.payment.*;
import shopping.international.domain.service.payment.IAdminPaymentService;
import shopping.international.types.exceptions.NotFoundException;

/**
 * 管理侧支付领域服务实现
 */
@Service
@RequiredArgsConstructor
public class AdminPaymentService implements IAdminPaymentService {

    /**
     * 管理侧支付读模型仓储
     */
    private final IAdminPaymentRepository repository;

    /**
     * 分页查询支付单 (管理侧)
     *
     * @param criteria  查询条件 包括但不限于支付单号, 支付状态等 {@link AdminPaymentSearchCriteria}
     * @param pageQuery 分页参数 包含了当前页码和每页显示条数 {@link PageQuery}
     * @return 返回分页后的支付单列表视图结果, 包含了满足条件的支付单信息以及分页相关的信息 {@link PageResult<AdminPaymentListItemView>}
     */
    @Override
    public @NotNull PageResult<AdminPaymentListItemView> pagePayments(@NotNull AdminPaymentSearchCriteria criteria, @NotNull PageQuery pageQuery) {
        criteria.validate();
        pageQuery.validate();
        return repository.pagePayments(criteria, pageQuery);
    }

    /**
     * 查询支付单详情 (管理侧)
     *
     * @param paymentId 支付单的唯一标识符 用于定位具体的支付记录
     * @return 返回一个 {@link AdminPaymentDetail} 对象, 包含了指定支付单的详细信息, 如果没有找到对应的支付单, 则抛出未找到异常
     */
    @Override
    public @NotNull AdminPaymentDetail getPaymentDetail(@NotNull Long paymentId) {
        return repository.getPaymentDetail(paymentId)
                .orElseThrow(() -> new NotFoundException("支付单不存在"));
    }

    /**
     * 分页查询退款单 (管理侧)
     *
     * @param criteria  查询条件 包括但不限于退款单号, 退款状态等 {@link AdminRefundSearchCriteria}
     * @param pageQuery 分页参数 包含了当前页码和每页显示条数 {@link PageQuery}
     * @return 返回分页后的退款单列表视图结果, 包含了满足条件的退款单信息以及分页相关的信息 {@link PageResult<AdminRefundListItemView>}
     */
    @Override
    public @NotNull PageResult<AdminRefundListItemView> pageRefunds(@NotNull AdminRefundSearchCriteria criteria, @NotNull PageQuery pageQuery) {
        criteria.validate();
        pageQuery.validate();
        return repository.pageRefunds(criteria, pageQuery);
    }

    /**
     * 查询退款单详情 (管理侧)
     *
     * @param refundId 退款单的唯一标识符, 用于定位具体的退款记录
     * @return 返回一个 {@link AdminRefundDetail} 对象, 包含了指定退款单的详细信息, 如果没有找到对应的退款单, 则抛出未找到异常
     */
    @Override
    public @NotNull AdminRefundDetail getRefundDetail(@NotNull Long refundId) {
        return repository.getRefundDetail(refundId)
                .orElseThrow(() -> new NotFoundException("退款单不存在"));
    }
}

