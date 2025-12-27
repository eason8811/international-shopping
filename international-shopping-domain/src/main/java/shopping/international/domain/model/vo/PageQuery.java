package shopping.international.domain.model.vo;

import org.jetbrains.annotations.NotNull;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 分页查询参数值对象
 *
 * <p>约定:</p>
 * <ul>
 *     <li>{@code page} 从 1 开始</li>
 *     <li>{@code size} 为单页数量</li>
 * </ul>
 */
public record PageQuery(int page, int size) implements Verifiable {

    /**
     * 创建分页查询参数, 并按上限进行裁剪
     *
     * <p>该方法用于在边界层进行“容错裁剪”, 避免出现 page/size 传入 0 或过大导致异常</p>
     *
     * @param page    页码 (从 1 开始)
     * @param size    每页大小
     * @param maxSize 单页最大数量
     * @return 分页查询参数
     */
    public static @NotNull PageQuery of(int page, int size, int maxSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        if (maxSize > 0)
            safeSize = Math.min(safeSize, maxSize);
        return new PageQuery(safePage, safeSize);
    }

    /**
     * 计算偏移量 (从 0 开始)
     *
     * @return offset
     */
    public int offset() {
        return (page - 1) * size;
    }

    /**
     * 计算 limit
     *
     * @return limit
     */
    public int limit() {
        return size;
    }

    /**
     * 校验分页参数合法性
     *
     * <p>当 {@link #of(int, int, int)} 已做裁剪时, 本方法通常不会抛错</p>
     */
    @Override
    public void validate() {
        require(page >= 1, "page 必须从 1 开始");
        require(size >= 1, "size 必须大于 0");
    }
}

