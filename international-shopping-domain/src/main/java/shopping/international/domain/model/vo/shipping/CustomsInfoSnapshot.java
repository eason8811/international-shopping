package shopping.international.domain.model.vo.shipping;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 关务信息快照值对象
 *
 * <p>该值对象采用动态键值结构, 用于兼容不同承运商和清关渠道的字段差异</p>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class CustomsInfoSnapshot implements Verifiable {

    /**
     * 动态关务字段映射
     */
    private final Map<String, Object> extra;

    /**
     * 构造关务信息快照
     *
     * @param extra 动态关务字段映射
     */
    private CustomsInfoSnapshot(Map<String, Object> extra) {
        this.extra = Collections.unmodifiableMap(new LinkedHashMap<>(extra));
    }

    /**
     * 创建空的关务信息快照
     *
     * @return 空快照值对象
     */
    public static @NotNull CustomsInfoSnapshot empty() {
        return new CustomsInfoSnapshot(Map.of());
    }

    /**
     * 根据动态字段创建关务信息快照
     *
     * @param extra 动态字段映射, 为空时按空快照处理
     * @return 关务信息快照值对象
     */
    public static @NotNull CustomsInfoSnapshot of(@Nullable Map<String, Object> extra) {
        CustomsInfoSnapshot snapshot = new CustomsInfoSnapshot(extra == null ? Map.of() : extra);
        snapshot.validate();
        return snapshot;
    }

    /**
     * 基于当前快照合并补丁字段并返回新对象
     *
     * @param patch 待合并的补丁字段
     * @return 合并后的新快照值对象
     */
    public @NotNull CustomsInfoSnapshot merge(@Nullable Map<String, Object> patch) {
        if (patch == null || patch.isEmpty())
            return this;
        Map<String, Object> merged = new LinkedHashMap<>(extra);
        merged.putAll(patch);
        return CustomsInfoSnapshot.of(merged);
    }

    /**
     * 校验快照字段合法性
     *
     * <p>当前仅校验字段名不能为空白且长度不超过 128</p>
     */
    @Override
    public void validate() {
        for (String key : extra.keySet()) {
            require(key != null, "customsInfo 字段名不能为空");
            String normalized = key.strip();
            require(!normalized.isEmpty(), "customsInfo 字段名不能为空白");
            require(normalized.length() <= 128, "customsInfo 字段名长度不能超过 128 个字符");
        }
    }
}
