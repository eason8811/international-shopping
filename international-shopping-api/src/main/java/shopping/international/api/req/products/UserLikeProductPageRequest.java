package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 用户点赞商品列表请求
 *
 * <p>承载 /users/me/likes/products 接口的分页与本地化参数</p>
 */
@Data
public class UserLikeProductPageRequest implements Verifiable {
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
     * 目标语言
     */
    @Nullable
    private String locale;
    /**
     * 价格币种
     */
    @Nullable
    private String currency;

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
    }
}
