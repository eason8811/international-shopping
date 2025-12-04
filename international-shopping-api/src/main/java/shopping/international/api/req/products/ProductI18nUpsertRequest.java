package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 商品多语言 upsert 请求 (ProductI18nUpsertRequest)
 */
@Data
public class ProductI18nUpsertRequest implements Verifiable {
    /**
     * 语言代码, 如 en-US
     */
    @NotNull
    private String locale;
    /**
     * 本地化标题, 长度不超过 255
     */
    @Nullable
    private String title;
    /**
     * 本地化副标题, 长度不超过 255, 可选
     */
    @Nullable
    private String subtitle;
    /**
     * 本地化描述, 可选
     */
    @Nullable
    private String description;
    /**
     * 本地化 slug, 长度不超过 120
     */
    @Nullable
    private String slug;
    /**
     * 本地化标签列表, 可选
     */
    @Nullable
    private List<String> tags;

    /**
     * 校验并规范化字段
     *
     * @throws IllegalParamException 当 locale、title 或 slug 无效时抛出
     */
    public void validate() {
        locale = normalizeNotNullField(locale, "语言代码不能为空", l -> l.length() <= 16, "语言代码格式不合法");
        locale = normalizeLocale(locale);
        subtitle = normalizeNullableField(subtitle, "subtitle 不能为空", s -> s.length() <= 255, "本地化副标题长度不能超过 255 个字符");
        description = normalizeNullableField(description, "description 不能为空", s -> true, null);
        normalizeTags();
    }

    /**
     * 默认调用 {@link #validate()} 方法来验证当前对象是否符合预定义的规则或条件
     *
     * @throws IllegalArgumentException 如果验证失败, 表示当前对象不符合要求, 异常信息将提供具体的错误详情
     */
    @Override
    public void createValidate() {
        validate();
        title = normalizeNotNullField(title, "title 不能为空", s -> s.length() <= 255, "本地化标题长度不能超过 255 个字符");
        slug = normalizeNotNullField(slug, "slug 不能为空", s -> s.length() <= 120, "本地化 slug 长度不能超过 120 个字符");
    }

    /**
     * 默认调用 {@link #validate()} 方法来验证当前对象在更新操作前是否符合预定义的规则或条件
     *
     * @throws IllegalArgumentException 如果验证失败, 表示当前对象不符合要求, 异常信息将提供具体的错误详情
     */
    @Override
    public void updateValidate() {
        validate();
        title = normalizeNullableField(title, "title 不能为空", s -> s.length() <= 255, "本地化标题长度不能超过 255 个字符");
        slug = normalizeNullableField(slug, "slug 不能为空", s -> s.length() <= 120, "本地化 slug 长度不能超过 120 个字符");
    }

    /**
     * 规范化标签列表, 确保每个标签都是唯一的, 并且去除首尾空白
     *
     * <p>此方法会对 {@code tags} 字段中的每一个标签执行以下操作:</p>
     * <ul>
     *     <li>如果标签为 null 或者空字符串(去除首尾空白后), 则跳过该标签</li>
     *     <li>检查标签长度是否超过 120 个字符, 如果超过则抛出异常</li>
     *     <li>确保所有标签唯一, 重复的标签将被忽略</li>
     * </ul>
     *
     * @throws IllegalParamException 当存在长度超过 120 个字符的标签时抛出
     */
    private void normalizeTags() {
        if (tags == null)
            return;
        List<String> normalized = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag == null)
                continue;
            String trimmed = tag.strip();
            if (trimmed.isEmpty())
                continue;
            if (trimmed.length() > 120)
                throw new IllegalParamException("本地化标签长度不能超过 120 个字符");
            if (seen.add(trimmed))
                normalized.add(trimmed);
        }
        tags = normalized;
    }
}
