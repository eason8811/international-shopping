package shopping.international.domain.model.entity.shipping;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.shipping.ShippingClaimReasonCode;
import shopping.international.domain.model.enums.shipping.ShippingClaimStatus;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.normalizeCurrency;
import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;
import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;
import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 物流理赔单实体
 *
 * <p>对应表 {@code shipping_claim}, 用于追踪承运商侧理赔处理进度</p>
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@Accessors(chain = true)
public class ShippingClaim implements Verifiable {

    /**
     * 主键 ID, 未持久化时可为空
     */
    @Nullable
    private Long id;
    /**
     * 工单 ID
     */
    @Nullable
    private Long ticketId;
    /**
     * 物流单 ID
     */
    private Long shipmentId;
    /**
     * 订单 ID
     */
    private Long orderId;
    /**
     * 补发单 ID
     */
    @Nullable
    private Long reshipId;
    /**
     * 承运商编码
     */
    private String carrierCode;
    /**
     * 承运商理赔外部编号
     */
    @Nullable
    private String externalId;
    /**
     * 理赔原因
     */
    private ShippingClaimReasonCode reasonCode;
    /**
     * 理赔状态
     */
    private ShippingClaimStatus status;
    /**
     * 申领理赔金额
     */
    private long claimAmount;
    /**
     * 已支付金额
     */
    @Nullable
    private Long paidAmount;
    /**
     * 币种
     */
    private String currency;
    /**
     * 支付时间
     */
    @Nullable
    private LocalDateTime paidAt;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 构造物流理赔实体
     *
     * @param id          主键 ID
     * @param ticketId    工单 ID
     * @param shipmentId  物流单 ID
     * @param orderId     订单 ID
     * @param reshipId    补发单 ID
     * @param carrierCode 承运商编码
     * @param externalId  外部理赔编号
     * @param reasonCode  理赔原因
     * @param status      理赔状态
     * @param claimAmount 申领理赔金额
     * @param paidAmount  已支付金额
     * @param currency    币种
     * @param paidAt      支付时间
     * @param createdAt   创建时间
     * @param updatedAt   更新时间
     */
    private ShippingClaim(@Nullable Long id,
                          @Nullable Long ticketId,
                          Long shipmentId,
                          Long orderId,
                          @Nullable Long reshipId,
                          String carrierCode,
                          @Nullable String externalId,
                          ShippingClaimReasonCode reasonCode,
                          ShippingClaimStatus status,
                          long claimAmount,
                          @Nullable Long paidAmount,
                          String currency,
                          @Nullable LocalDateTime paidAt,
                          LocalDateTime createdAt,
                          LocalDateTime updatedAt) {
        this.id = id;
        this.ticketId = ticketId;
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.reshipId = reshipId;
        this.carrierCode = carrierCode;
        this.externalId = externalId;
        this.reasonCode = reasonCode;
        this.status = status;
        this.claimAmount = claimAmount;
        this.paidAmount = paidAmount;
        this.currency = currency;
        this.paidAt = paidAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 发起新理赔单
     *
     * @param ticketId    工单 ID
     * @param shipmentId  物流单 ID
     * @param orderId     订单 ID
     * @param reshipId    补发单 ID
     * @param carrierCode 承运商编码
     * @param reasonCode  理赔原因
     * @param claimAmount 理赔金额
     * @param currency    币种
     * @return 新建理赔单实体
     */
    public static ShippingClaim create(@Nullable Long ticketId,
                                       Long shipmentId,
                                       Long orderId,
                                       @Nullable Long reshipId,
                                       String carrierCode,
                                       ShippingClaimReasonCode reasonCode,
                                       long claimAmount,
                                       @Nullable String currency) {
        ShippingClaim claim = new ShippingClaim(
                null, ticketId, shipmentId, orderId, reshipId,
                carrierCode, null, reasonCode, ShippingClaimStatus.FILED,
                claimAmount, null, currency, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
        claim.validate();
        return claim;
    }

    /**
     * 从持久化层重建理赔单实体
     *
     * @param id          主键 ID
     * @param ticketId    工单 ID
     * @param shipmentId  物流单 ID
     * @param orderId     订单 ID
     * @param reshipId    补发单 ID
     * @param carrierCode 承运商编码
     * @param externalId  外部理赔编号
     * @param reasonCode  理赔原因
     * @param status      理赔状态
     * @param claimAmount 理赔金额
     * @param paidAmount  已支付金额
     * @param currency    币种
     * @param paidAt      支付时间
     * @param createdAt   创建时间
     * @param updatedAt   更新时间
     * @return 重建后的理赔单实体
     */
    public static ShippingClaim reconstitute(Long id,
                                             @Nullable Long ticketId,
                                             Long shipmentId,
                                             Long orderId,
                                             @Nullable Long reshipId,
                                             String carrierCode,
                                             @Nullable String externalId,
                                             ShippingClaimReasonCode reasonCode,
                                             ShippingClaimStatus status,
                                             long claimAmount,
                                             @Nullable Long paidAmount,
                                             String currency,
                                             @Nullable LocalDateTime paidAt,
                                             LocalDateTime createdAt,
                                             LocalDateTime updatedAt) {
        ShippingClaim claim = new ShippingClaim(id, ticketId, shipmentId, orderId, reshipId,
                carrierCode, externalId, reasonCode, status, claimAmount, paidAmount,
                currency, paidAt, createdAt, updatedAt);
        claim.validate();
        return claim;
    }

    /**
     * 绑定承运商理赔外部编号
     *
     * @param externalId 外部理赔编号
     */
    public void bindExternalId(@Nullable String externalId) {
        this.externalId = normalizeNullableField(externalId, "externalId 不能为空",
                value -> value.length() <= 128,
                "externalId 长度不能超过 128 个字符");
        touch();
    }

    /**
     * 状态推进到审核中
     */
    public void markUnderReview() {
        if (status == ShippingClaimStatus.UNDER_REVIEW)
            return;
        requireStatus(status == ShippingClaimStatus.FILED, "仅 FILED 状态允许进入 UNDER_REVIEW");
        status = ShippingClaimStatus.UNDER_REVIEW;
        touch();
    }

    /**
     * 审核通过
     */
    public void approve() {
        if (status == ShippingClaimStatus.APPROVED)
            return;
        requireStatus(status == ShippingClaimStatus.UNDER_REVIEW, "仅 UNDER_REVIEW 状态允许进入 APPROVED");
        status = ShippingClaimStatus.APPROVED;
        touch();
    }

    /**
     * 审核拒绝
     */
    public void reject() {
        if (status == ShippingClaimStatus.REJECTED)
            return;
        requireStatus(status == ShippingClaimStatus.UNDER_REVIEW, "仅 UNDER_REVIEW 状态允许进入 REJECTED");
        status = ShippingClaimStatus.REJECTED;
        touch();
    }

    /**
     * 标记理赔打款完成
     *
     * @param paidAmount 已支付金额
     * @param paidAt     支付时间
     */
    public void markPaid(long paidAmount, LocalDateTime paidAt) {
        if (status == ShippingClaimStatus.PAID)
            return;
        requireStatus(status == ShippingClaimStatus.APPROVED, "仅 APPROVED 状态允许进入 PAID");
        require(paidAmount > 0, "paidAmount 必须大于 0");
        require(paidAmount <= claimAmount, "paidAmount 不能大于 claimAmount");
        requireNotNull(paidAt, "paidAt 不能为空");
        this.paidAmount = paidAmount;
        this.paidAt = paidAt;
        this.status = ShippingClaimStatus.PAID;
        touch();
    }

    /**
     * 关闭理赔单
     */
    public void close() {
        if (status == ShippingClaimStatus.CLOSED)
            return;
        requireStatus(status == ShippingClaimStatus.REJECTED
                        || status == ShippingClaimStatus.PAID
                        || status == ShippingClaimStatus.APPROVED,
                "仅 REJECTED/APPROVED/PAID 状态允许关闭");
        status = ShippingClaimStatus.CLOSED;
        touch();
    }

    /**
     * 断言状态机条件
     *
     * @param ok      条件表达式
     * @param message 条件失败提示
     */
    private static void requireStatus(boolean ok, String message) {
        if (!ok)
            throw new ConflictException(message);
    }

    /**
     * 更新时间戳
     */
    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 校验理赔单实体不变式
     */
    @Override
    public void validate() {
        if (id != null)
            require(id > 0, "id 必须大于 0");
        if (ticketId != null)
            require(ticketId > 0, "ticketId 必须大于 0");
        if (reshipId != null)
            require(reshipId > 0, "reshipId 必须大于 0");

        requireNotNull(shipmentId, "shipmentId 不能为空");
        require(shipmentId > 0, "shipmentId 必须大于 0");
        requireNotNull(orderId, "orderId 不能为空");
        require(orderId > 0, "orderId 必须大于 0");

        carrierCode = normalizeNotNullField(carrierCode, "carrierCode 不能为空",
                value -> value.length() <= 64,
                "carrierCode 长度不能超过 64 个字符");
        externalId = normalizeNullableField(externalId, "externalId 不能为空",
                value -> value.length() <= 128,
                "externalId 长度不能超过 128 个字符");

        requireNotNull(reasonCode, "reasonCode 不能为空");
        requireNotNull(status, "status 不能为空");

        require(claimAmount > 0, "claimAmount 必须大于 0");
        if (paidAmount != null) {
            require(paidAmount >= 0, "paidAmount 不能为负数");
            require(paidAmount <= claimAmount, "paidAmount 不能大于 claimAmount");
        }

        currency = normalizeCurrency(currency);

        if (status == ShippingClaimStatus.PAID) {
            requireNotNull(paidAmount, "PAID 状态下 paidAmount 不能为空");
            require(paidAmount > 0, "PAID 状态下 paidAmount 必须大于 0");
            requireNotNull(paidAt, "PAID 状态下 paidAt 不能为空");
        }

        requireNotNull(createdAt, "createdAt 不能为空");
        requireNotNull(updatedAt, "updatedAt 不能为空");
    }
}
