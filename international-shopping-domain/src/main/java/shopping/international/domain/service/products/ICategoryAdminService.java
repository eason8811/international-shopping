package shopping.international.domain.service.products;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.vo.products.CategoryI18n;
import shopping.international.domain.model.vo.products.CategoryNode;
import shopping.international.domain.model.vo.products.CategoryUpsertCommand;

import java.util.List;

/**
 * 商品分类管理服务
 *
 * <p>提供管理端的分类分页、详情、创建、更新、多语言维护及启用状态切换能力</p>
 */
public interface ICategoryAdminService {

    /**
     * 分页查询分类
     *
     * @param page           页码
     * @param size           每页大小
     * @param filterByParent 是否按父级过滤
     * @param parentId       父级 ID
     * @param keyword        关键词
     * @param isEnabled      是否启用
     * @return 分页结果
     */
    @NotNull
    PageResult<CategoryNode> page(int page, int size, boolean filterByParent, Long parentId, String keyword, Boolean isEnabled);

    /**
     * 查询分类详情
     *
     * @param categoryId 分类 ID
     * @return 分类视图
     */
    @NotNull
    CategoryNode detail(@NotNull Long categoryId);

    /**
     * 创建分类
     *
     * @param command 创建命令
     * @return 创建后的分类
     */
    @NotNull
    CategoryNode create(@NotNull CategoryUpsertCommand command);

    /**
     * 更新分类
     *
     * @param categoryId 分类 ID
     * @param command    更新命令
     * @return 更新后的分类
     */
    @NotNull
    CategoryNode update(@NotNull Long categoryId, @NotNull CategoryUpsertCommand command);

    /**
     * 批量 upsert 分类多语言
     *
     * @param categoryId 分类 ID
     * @param payloads   多语言列表
     * @return 更新后的分类
     */
    @NotNull
    CategoryNode upsertI18n(@NotNull Long categoryId, @NotNull List<CategoryI18n> payloads);

    /**
     * 启用或停用分类
     *
     * @param categoryId 分类 ID
     * @param enabled    目标状态
     * @return 更新后的分类
     */
    @NotNull
    CategoryNode toggleEnable(@NotNull Long categoryId, boolean enabled);

    /**
     * 删除分类
     *
     * @param categoryId 分类 ID
     */
    void delete(@NotNull Long categoryId);

    /**
     * 简单分页结果
     *
     * @param items 列表
     * @param total 总数
     * @param <T>   数据类型
     */
    record PageResult<T>(List<T> items, long total) {
    }
}
