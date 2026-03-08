package shopping.international.infrastructure.dao.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.user.po.UserAddressExtPO;

import java.util.List;

/**
 * Mapper: user_address_ext
 */
@Mapper
public interface UserAddressExtMapper extends BaseMapper<UserAddressExtPO> {

    /**
     * 按地址 ID 批量插入或更新地址扩展信息。
     *
     * <p>当 {@code address_id} 已存在时, 仅覆盖扩展字段, 保留数据库维护的时间戳语义。</p>
     *
     * @param items 待写入的地址扩展记录
     * @return 受影响行数
     */
    int upsertBatch(@Param("items") List<UserAddressExtPO> items);
}
