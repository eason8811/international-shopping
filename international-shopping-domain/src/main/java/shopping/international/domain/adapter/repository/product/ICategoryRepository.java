package shopping.international.domain.adapter.repository.product;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.products.Category;
import shopping.international.domain.model.enums.products.CategoryStatus;
import shopping.international.domain.model.vo.products.CategoryI18n;

import java.util.List;
import java.util.Optional;

/**
 * 商品分类聚合的仓储接口
 *
 * <p>封装对 {@code product_category} 及其 i18n 表的组合读写, 返回和接收的都是领域聚合 {@link Category}</p>
 */
public interface ICategoryRepository {

    /**
     * 按 ID 查询分类 (含 i18n)
     *
     * @param categoryId 分类 ID
     * @return 聚合, 可为空
     */
    Optional<Category> findById(@NotNull Long categoryId);

    /**
     * 按条件列出全量分类, 常用于构建树
     *
     * @param status 状态过滤, 为空则不过滤
     * @return 按 sortOrder、id 升序的分类列表, 包含 i18n
     */
    @NotNull
    List<Category> listAll(@Nullable CategoryStatus status);

    /**
     * 分页列出分类
     *
     * @param parentId        父分类 ID, 可空
     * @param parentSpecified 是否对 parentId 进行过滤 (区分 "未传" 与 "明确为 null" )
     * @param keyword         关键词 (匹配 name/slug), 可空
     * @param status          启用状态过滤, 可空
     * @param offset          偏移量, 从 0 开始
     * @param limit           单页数量
     * @return 当前页的分类列表
     */
    @NotNull
    List<Category> list(@Nullable Long parentId, boolean parentSpecified, @Nullable String keyword,
                        @Nullable CategoryStatus status, int offset, int limit);

    /**
     * 统计满足条件的分类数量
     *
     * @param parentId        父分类 ID, 可空
     * @param parentSpecified 是否对 parentId 进行过滤
     * @param keyword         关键词
     * @param status          状态过滤
     * @return 总数
     */
    long count(@Nullable Long parentId, boolean parentSpecified, @Nullable String keyword,
               @Nullable CategoryStatus status);

    /**
     * 检查 slug 是否已存在
     *
     * @param slug              唯一 slug
     * @param excludeCategoryId 需要排除的分类 ID, 可空
     * @return 是否存在
     */
    boolean existsBySlug(@NotNull String slug, @Nullable Long excludeCategoryId);

    /**
     * 检查同一父级下名称是否重复
     *
     * @param parentId          父分类 ID, 可空表示根
     * @param name              分类名称
     * @param excludeCategoryId 需要排除的分类 ID, 可空
     * @return 是否存在重名
     */
    boolean existsByParentAndName(@Nullable Long parentId, @NotNull String name, @Nullable Long excludeCategoryId);

    /**
     * 新增分类 (含 i18n)
     *
     * @param category 待保存的新聚合, ID 为空
     * @return 带持久化 ID 的聚合
     */
    @NotNull
    Category save(@NotNull Category category);

    /**
     * 更新分类基础信息及可选的 i18n
     *
     * @param category    已存在的聚合快照
     * @param replaceI18n 是否重写 i18n 表 (为 {@code false} 时不改 i18n)
     * @param moveContext 父级变更时传入, 用于批量调整子节点层级与路径, 可空
     * @return 更新后的聚合
     */
    @NotNull
    Category update(@NotNull Category category, boolean replaceI18n, @Nullable MoveContext moveContext);

    /**
     * 删除分类及其 i18n
     *
     * @param categoryId 分类 ID
     */
    void delete(@NotNull Long categoryId);

    /**
     * 判断是否存在子分类
     *
     * @param categoryId 分类 ID
     * @return 是否有子节点
     */
    boolean hasChildren(@NotNull Long categoryId);

    /**
     * 判断分类下是否存在商品
     *
     * @param categoryId 分类 ID
     * @return 是否存在商品引用
     */
    boolean hasProducts(@NotNull Long categoryId);

    /**
     * 新增单条多语言记录
     *
     * @param categoryId 分类 ID
     * @param i18n       多语言值对象
     */
    void saveI18n(@NotNull Long categoryId, @NotNull CategoryI18n i18n);

    /**
     * 更新单条多语言记录
     *
     * @param categoryId 分类 ID
     * @param i18n       新值对象
     */
    void updateI18n(@NotNull Long categoryId, @NotNull CategoryI18n i18n);

    /**
     * 列出指定分类的全部多语言
     *
     * @param categoryId 分类 ID
     * @return 多语言列表
     */
    @NotNull
    List<CategoryI18n> listI18nByCategoryId(@NotNull Long categoryId);

    /**
     * 父级变更的上下文, 供仓储批量调整子树
     *
     * @param oldPrefix  旧的子树路径前缀 (如 /1/2/), 用于匹配并替换子节点 path
     * @param newPrefix  新的子树路径前缀 (如 /3/2/), 用于替换子节点 path
     * @param levelDelta 层级调整量 (新 level - 旧 level)
     */
    record MoveContext(String oldPrefix, String newPrefix, int levelDelta) {
    }
}
