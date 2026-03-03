package shopping.international.infrastructure.dao.customerservice;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.customerservice.po.AfterSalesReshipItemPO;
import shopping.international.infrastructure.dao.customerservice.po.AfterSalesReshipPO;
import shopping.international.infrastructure.dao.customerservice.po.AfterSalesReshipShipmentPO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper, aftersales_reship 表
 */
@Mapper
public interface AfterSalesReshipMapper extends BaseMapper<AfterSalesReshipPO> {

    /**
     * 分页查询管理侧补发单
     *
     * @param reshipNo    补发单号
     * @param orderId     订单 ID
     * @param ticketId    工单 ID
     * @param status      补发状态
     * @param reasonCode  补发原因
     * @param createdFrom 创建时间起始
     * @param createdTo   创建时间结束
     * @param offset      分页偏移量
     * @param limit       分页条数
     * @return 补发单列表
     */
    List<AfterSalesReshipPO> pageAdminReships(@Param("reshipNo") String reshipNo,
                                              @Param("orderId") Long orderId,
                                              @Param("ticketId") Long ticketId,
                                              @Param("status") String status,
                                              @Param("reasonCode") String reasonCode,
                                              @Param("createdFrom") LocalDateTime createdFrom,
                                              @Param("createdTo") LocalDateTime createdTo,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);

    /**
     * 统计管理侧补发单总数
     *
     * @param reshipNo    补发单号
     * @param orderId     订单 ID
     * @param ticketId    工单 ID
     * @param status      补发状态
     * @param reasonCode  补发原因
     * @param createdFrom 创建时间起始
     * @param createdTo   创建时间结束
     * @return 总数
     */
    long countAdminReships(@Param("reshipNo") String reshipNo,
                           @Param("orderId") Long orderId,
                           @Param("ticketId") Long ticketId,
                           @Param("status") String status,
                           @Param("reasonCode") String reasonCode,
                           @Param("createdFrom") LocalDateTime createdFrom,
                           @Param("createdTo") LocalDateTime createdTo);

    /**
     * 按补发单 ID 列表查询补发明细, 联表计算明细金额
     *
     * @param reshipIds 补发单 ID 列表
     * @return 补发明细列表
     */
    List<AfterSalesReshipItemPO> listReshipItemDetailsByReshipIds(@Param("reshipIds") List<Long> reshipIds);

    /**
     * 按补发单 ID 列表查询关联物流单
     *
     * @param reshipIds 补发单 ID 列表
     * @return 关联物流单列表
     */
    List<AfterSalesReshipShipmentPO> listReshipShipmentsByReshipIds(@Param("reshipIds") List<Long> reshipIds);
}
