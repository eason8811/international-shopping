package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.FieldValidateUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static shopping.international.types.utils.FieldValidateUtils.requireCreateField;
import static shopping.international.types.utils.FieldValidateUtils.requirePatchField;

/**
 * 商品多语言 upsert 请求 (ProductI18nUpsertRequest)
 */
@Data
public class ProductI18nUpsertRequest {
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
        locale = requireCreateField(locale,
                "语言代码不能为空",
                locale -> FieldValidateUtils.LOCALE_PATTERN.matcher(locale).matches(),
                "语言代码格式不合法");
        title = requirePatchField(title, "title 不能为空", s -> s.length() <= 255, "本地化标题长度不能超过 255 个字符");
        subtitle = requirePatchField(subtitle, "subtitle 不能为空", s -> s.length() <= 255, "本地化副标题长度不能超过 255 个字符");
        description = requirePatchField(description, "description 不能为空", s -> true, null);
        slug = requirePatchField(slug, "slug 不能为空", s -> s.length() <= 120, "本地化 slug 长度不能超过 120 个字符");
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
