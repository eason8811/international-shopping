package shopping.international.infrastructure.dao.orders;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.orders.po.CartItemViewPO;
import shopping.international.infrastructure.dao.orders.po.ShoppingCartItemPO;

import java.util.List;

/**
 * Mapper: shopping_cart_item
 *
 * <p>职责:</p>
 * <ul>
 *     <li>提供 {@link BaseMapper} 通用 CRUD</li>
 *     <li>提供用户侧购物车展示所需的联表视图查询</li>
 * </ul>
 */
@Mapper
public interface ShoppingCartItemMapper extends BaseMapper<ShoppingCartItemPO> {

    /**
     * 按用户分页查询购物车条目视图
     *
     * @param userId   用户 ID
     * @param offset   偏移量, 从 0 开始
     * @param limit    单页数量
     * @param locale   展示语言 (可为空)
     * @param currency 展示币种 (可为空)
     * @return 购物车条目视图列表
     */
    List<CartItemViewPO> selectCartItemViews(@Param("userId") Long userId,
                                             @Param("offset") int offset,
                                             @Param("limit") int limit,
                                             @Param("locale") String locale,
                                             @Param("currency") String currency);
}

