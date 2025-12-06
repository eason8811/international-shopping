package shopping.international.domain.service.products;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.products.Category;
import shopping.international.domain.model.vo.products.CategoryI18n;

import java.util.List;

/**
 * 商品分类领域服务接口
 *
 * <p>面向用例的分类查询与维护操作, 通过 {@link shopping.international.domain.adapter.repository.product.ICategoryRepository}
 * 与持久化层交互, 聚焦领域规则校验与聚合行为编排</p>
 */
public interface ICategoryService {

    /**
     * 简单分页结构
     *
     * @param items 当前页的分类列表
     * @param total 满足筛选条件的总数
     */
    @Builder
    record PageResult(List<Category> items, long total) {
    }

    /**
     * 多语言增量更新命令
     *
     * @param locale 语言代码
     * @param name   新名称, 可空表示不改
     * @param slug   新 slug, 可空表示不改
     * @param brand  新品牌文案, 可空表示不改
     */
    @Builder
    record CategoryI18nPatch(String locale, String name, String slug, String brand) {
    }

    /**
     * 列出全部启用分类 (含 i18n), 用于构建用户侧分类树
     *
     * @return 分类列表
     */
    @NotNull
    List<Category> listEnabled();

    /**
     * 分页筛选分类 (管理侧)
     *
     * @param page            页码, 从 1 开始
     * @param size            页大小
     * @param parentSpecified 是否按父级过滤
     * @param parentId        父级 ID, 可空
     * @param keyword         关键词, 可空
     * @param isEnabled       启用过滤, 可空
     * @return 分页结果
     */
    @NotNull
    PageResult list(int page, int size, boolean parentSpecified, @Nullable Long parentId,
                    @Nullable String keyword, @Nullable Boolean isEnabled);

    /**
     * 获取分类详情
     *
     * @param categoryId 分类 ID
     * @return 聚合
     */
    @NotNull
    Category get(@NotNull Long categoryId);

    /**
     * 创建分类
     *
     * @param name      名称
     * @param slug      唯一 slug
     * @param parentId  父分类 ID, 可空
     * @param sortOrder 排序
     * @param isEnabled 启用状态
     * @param i18nList  多语言列表, 可空
     * @return 新建聚合
     */
    @NotNull
    Category create(@NotNull String name, @NotNull String slug, @Nullable Long parentId,
                    @NotNull Integer sortOrder, @NotNull Boolean isEnabled,
                    @NotNull List<CategoryI18n> i18nList);

    /**
     * 更新分类基础信息及可选 i18n
     *
     * @param categoryId  分类 ID
     * @param name        新名称, 可空
     * @param slug        新 slug, 可空
     * @param parentId    新父级, 可空
     * @param sortOrder   新排序, 可空
     * @param isEnabled   新启用状态, 可空
     * @param i18nPatches 多语言增量列表, 可空
     * @return 更新后的聚合
     */
    @NotNull
    Category update(@NotNull Long categoryId, @Nullable String name, @Nullable String slug,
                    @Nullable Long parentId, @Nullable Integer sortOrder, @Nullable Boolean isEnabled,
                    @Nullable List<CategoryI18nPatch> i18nPatches);

    /**
     * 删除分类
     *
     * @param categoryId 分类 ID
     */
    void delete(@NotNull Long categoryId);

    /**
     * 新增多语言
     *
     * @param categoryId 分类 ID
     * @param i18n       多语言值对象
     * @return 新增后的值对象
     */
    @NotNull
    CategoryI18n addI18n(@NotNull Long categoryId, @NotNull CategoryI18n i18n);

    /**
     * 增量更新多语言
     *
     * @param categoryId 分类 ID
     * @param patch      多语言增量
     * @return 更新后的值对象
     */
    @NotNull
    CategoryI18n updateI18n(@NotNull Long categoryId, @NotNull CategoryI18nPatch patch);

    /**
     * 切换启用状态
     *
     * @param categoryId 分类 ID
     * @param enable     目标是否启用
     * @return 更新后的聚合
     */
    @NotNull
    Category toggleStatus(@NotNull Long categoryId, boolean enable);
}
