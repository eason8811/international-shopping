package shopping.international.domain.adapter.repository.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.vo.common.CurrencyProfile;

import java.util.List;

/**
 * 币种配置仓储接口 (currency 表) 
 */
public interface ICurrencyRepository {

    /**
     * 按币种代码查询 (可返回禁用币种，由上层决定是否使用) 
     *
     * @param code ISO 4217 代码 (如 USD) 
     * @return 币种配置，未找到返回 null
     */
    @Nullable
    CurrencyProfile findByCode(@NotNull String code);

    /**
     * 查询启用币种代码列表
     *
     * <p>用于 FX 同步与价格派生时的目标币种集合。</p>
     *
     * @return 启用币种代码列表 (可能为空)
     */
    @NotNull
    List<String> listEnabledCodes();
}
