package shopping.international.domain.model.vo;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 通用分页结果值对象
 *
 * @param items 当前页数据
 * @param total 总数
 * @param <T>   元素类型
 */
@Builder
public record PageResult<T>(@NotNull List<T> items, long total) {

    /**
     * 构造分页结果
     *
     * @param items 当前页数据
     * @param total 总数
     */
    public PageResult {
        items = List.copyOf(items);
    }
}

