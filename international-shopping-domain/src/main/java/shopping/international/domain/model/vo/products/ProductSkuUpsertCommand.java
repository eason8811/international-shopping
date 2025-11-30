package shopping.international.domain.model.vo.products;

import lombok.Getter;
import lombok.ToString;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 批量维护 SKU 的命令 ProductSkuUpsertCommand
 *
 * <p>聚合多个 SKU 条目, 用于批量创建或更新</p>
 */
@Getter
@ToString
public class ProductSkuUpsertCommand {
    /**
     * SKU 条目列表
     */
    private final List<ProductSkuUpsertItemCommand> items;

    private ProductSkuUpsertCommand(List<ProductSkuUpsertItemCommand> items) {
        this.items = items;
    }

    /**
     * 构建批量 SKU 维护命令
     *
     * @param items SKU 条目列表
     * @return 批量维护命令
     * @throws IllegalParamException 当列表为空时抛出 IllegalParamException
     */
    public static ProductSkuUpsertCommand of(List<ProductSkuUpsertItemCommand> items) {
        requireNotNull(items, "SKU 列表不能为空");
        if (items.isEmpty())
            throw new IllegalParamException("SKU 列表不能为空");
        return new ProductSkuUpsertCommand(List.copyOf(items));
    }
}
