package shopping.international.infrastructure.adapter.repository.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import shopping.international.domain.adapter.repository.common.ICurrencyRepository;
import shopping.international.domain.model.vo.common.CurrencyProfile;
import shopping.international.infrastructure.dao.common.CurrencyMapper;
import shopping.international.infrastructure.dao.common.po.CurrencyPO;

import java.util.Collections;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.normalizeCurrency;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * currency 公共表仓储实现
 */
@Repository
@RequiredArgsConstructor
public class CurrencyRepository implements ICurrencyRepository {

    /**
     * currency Mapper
     */
    private final CurrencyMapper currencyMapper;

    /**
     * 按币种代码查询 (可返回禁用币种，由上层决定是否使用)
     *
     * @param code ISO 4217 代码 (如 USD)
     * @return 币种配置快照, 如果未找到则返回 null
     */
    @Override
    public @Nullable CurrencyProfile findByCode(@NotNull String code) {
        String normalized = normalizeCurrency(code);
        requireNotNull(normalized, "currency 不能为空");
        CurrencyPO po = currencyMapper.selectById(normalized);
        if (po == null)
            return null;
        return new CurrencyProfile(
                po.getCode(),
                po.getMinorUnit(),
                po.getRoundingMode(),
                po.getCashRoundingInc(),
                po.getEnabled()
        );
    }

    /**
     * 查询启用币种代码列表
     *
     * @return 启用币种代码列表 (可能为空)
     */
    @Override
    public @NotNull List<String> listEnabledCodes() {
        List<CurrencyPO> pos = currencyMapper.selectList(
                new LambdaQueryWrapper<CurrencyPO>()
                        .eq(CurrencyPO::getEnabled, true)
                        .select(CurrencyPO::getCode)
        );
        if (pos == null || pos.isEmpty())
            return Collections.emptyList();
        return pos.stream()
                .map(CurrencyPO::getCode)
                .filter(c -> c != null && !c.isBlank())
                .map(String::strip)
                .toList();
    }
}
