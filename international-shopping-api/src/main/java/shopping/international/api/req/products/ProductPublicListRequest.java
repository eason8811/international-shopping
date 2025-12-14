package shopping.international.api.req.products;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.products.ProductSort;
import shopping.international.types.utils.Verifiable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 用户侧商品列表查询请求
 *
 * <p>承载 /products 接口的查询条件, 负责完成参数默认值处理与基础校验</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductPublicListRequest implements Verifiable {
    /**
     * Jackson 映射器, 用于解析标签 JSON
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    /**
     * 页码(从 1 开始)
     */
    @Nullable
    private Integer page = 1;
    /**
     * 每页数量
     */
    @Nullable
    private Integer size = 20;
    /**
     * 语言代码, 用于匹配多语言信息
     */
    @Nullable
    private String locale;
    /**
     * 结算币种, 用于匹配价格
     */
    @Nullable
    private String currency;
    /**
     * 分类 slug, 支持多语言 slug
     */
    @Nullable
    private String categorySlug;
    /**
     * 关键词, 匹配标题/副标题/描述/slug
     */
    @Nullable
    private String query;
    /**
     * 标签过滤, 支持 JSON 数组或逗号分隔字符串
     */
    @Nullable
    private String tags;
    /**
     * 价格下限(与 currency 同币种)
     */
    @Nullable
    private BigDecimal priceMin;
    /**
     * 价格上限(与 currency 同币种)
     */
    @Nullable
    private BigDecimal priceMax;
    /**
     * 排序方式
     */
    @Nullable
    private ProductSort sortBy;
    /**
     * 解析后的标签列表
     */
    @NotNull
    private List<String> parsedTags = new ArrayList<>();

    /**
     * 校验并规范化请求参数
     */
    @Override
    public void validate() {
        if (page == null || page < 1)
            page = 1;
        if (size == null || size < 1)
            size = 20;
        if (size > 100)
            size = 100;
        locale = normalizeLocale(normalizeNotNullField(locale, "locale 不能为空", s -> true, null));
        currency = normalizeCurrency(normalizeNotNullField(currency, "currency 不能为空", s -> true, null));
        categorySlug = normalizeNullableField(categorySlug, "category_slug 不能为空", slug -> slug.length() <= 120, "category_slug 长度不能超过 120 个字符");
        query = normalizeKeyword(query, 255);
        priceMin = normalizeNonNegativePrice(priceMin, "price_min");
        priceMax = normalizeNonNegativePrice(priceMax, "price_max");
        if (priceMin != null && priceMax != null)
            require(priceMin.compareTo(priceMax) <= 0, "price_min 不能大于 price_max");
        if (sortBy == null)
            sortBy = ProductSort.LATEST;
        parsedTags = parseTags();
    }

    /**
     * 将输入的 tags 字符串解析为去重后的标签列表
     *
     * @return 规范化后的标签列表
     */
    private List<String> parseTags() {
        if (tags == null || tags.isBlank())
            return List.of();
        String trimmed = tags.trim();
        List<String> rawTags = new ArrayList<>();
        if (trimmed.startsWith("[")) {
            try {
                List<String> parsed = OBJECT_MAPPER.readValue(trimmed, new TypeReference<>() {
                });
                rawTags.addAll(parsed);
            } catch (Exception ignore) {
                rawTags.add(trimmed);
            }
        } else {
            rawTags.addAll(Arrays.asList(trimmed.split(",")));
        }
        return normalizeTags(rawTags);
    }

    public void setPage(@Nullable Integer page) {
        this.page = page;
    }

    public void setSize(@Nullable Integer size) {
        this.size = size;
    }

    public void setLocale(@Nullable String locale) {
        this.locale = locale;
    }

    public void setCurrency(@Nullable String currency) {
        this.currency = currency;
    }

    public void setCategory_slug(@Nullable String categorySlug) {
        this.categorySlug = categorySlug;
    }

    public void setQuery(@Nullable String query) {
        this.query = query;
    }

    public void setTags(@Nullable String tags) {
        this.tags = tags;
    }

    public void setPrice_min(@Nullable BigDecimal priceMin) {
        this.priceMin = priceMin;
    }

    public void setPrice_max(@Nullable BigDecimal priceMax) {
        this.priceMax = priceMax;
    }

    public void setSort_by(@Nullable ProductSort sortBy) {
        this.sortBy = sortBy;
    }

    public void setParsed_tags(@NotNull List<String> parsedTags) {
        this.parsedTags = parsedTags;
    }
}
