package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 商品多语言 upsert 请求 (ProductI18nUpsertRequest)
 */
@Data
public class ProductI18nUpsertRequest {
    /**
     * 语言代码, 如 en-US
     */
    private String locale;
    /**
     * 本地化标题, 长度不超过 255
     */
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
        requireNotBlank(locale, "语言代码不能为空");
        locale = locale.strip();
        if (!LOCALE_PATTERN.matcher(locale).matches())
            throw new IllegalParamException("语言代码格式不合法");

        requireNotBlank(title, "本地化标题不能为空");
        title = title.strip();
        if (title.length() > 255)
            throw new IllegalParamException("本地化标题长度不能超过 255 个字符");

        if (subtitle != null) {
            subtitle = subtitle.strip();
            if (subtitle.length() > 255)
                throw new IllegalParamException("本地化副标题长度不能超过 255 个字符");
        }
        if (description != null)
            description = description.strip();

        requireNotBlank(slug, "本地化 slug 不能为空");
        slug = slug.strip();
        if (slug.length() > 120)
            throw new IllegalParamException("本地化 slug 长度不能超过 120 个字符");

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

    /**
     * 多语言代码校验正则
     */
    private static final Pattern LOCALE_PATTERN = Pattern.compile("^[A-Za-z0-9]{2,8}([-_][A-Za-z0-9]{2,8})*$");
}
