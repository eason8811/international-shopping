// UserAddressMapper.java
package shopping.international.infrastructure.dao.user;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.user.po.UserAddressPO;

/**
 * Mapper: user_address
 * <p>继承 BaseMapper, 提供通用 CRUD</p>
 */
@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddressPO> {
}
