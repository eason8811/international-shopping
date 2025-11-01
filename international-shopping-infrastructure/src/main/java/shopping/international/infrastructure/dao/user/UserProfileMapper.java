package shopping.international.infrastructure.dao.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.user.po.UserProfilePO;

/**
 * Mapper：user_profile
 * <p>继承 BaseMapper，提供通用 CRUD。</p>
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfilePO> {
}
