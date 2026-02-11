package shopping.international.api.resp.shipping;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 关务信息快照响应对象 (CustomsInfoSnapshotRespond)
 *
 * <p>该对象采用动态键值结构，以兼容不同承运商的关务字段</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomsInfoSnapshotRespond {
    /**
     * 动态关务字段集合
     */
    @Builder.Default
    private Map<String, Object> extra = new LinkedHashMap<>();

    /**
     * 写入未显式声明的关务字段
     *
     * @param key   字段名
     * @param value 字段值
     */
    @JsonAnySetter
    public void putExtra(@NotNull String key, Object value) {
        extra.put(key, value);
    }

    /**
     * 输出动态关务字段集合
     *
     * @return 动态关务字段映射
     */
    @JsonAnyGetter
    public Map<String, Object> extra() {
        return extra;
    }
}
