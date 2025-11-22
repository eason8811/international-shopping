package shopping.international.trigger.controller.products;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.products.CategoryNodeRespond;
import shopping.international.domain.service.products.ICategoryQueryService;
import shopping.international.types.constant.SecurityConstants;

import java.util.List;

/**
 * 商品分类接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/products/categories")
public class CategoryController {

    /**
     * 分类查询服务
     */
    private final ICategoryQueryService categoryQueryService;

    /**
     * 列出启用分类树, 可按 locale 返回本地化字段
     *
     * @param locale 语言代码 (如 en-US), 可空
     * @return 分类树
     */
    @GetMapping("/tree")
    public ResponseEntity<Result<List<CategoryNodeRespond>>> tree(@RequestParam(value = "locale", required = false) String locale) {
        List<CategoryNodeRespond> data = categoryQueryService.tree(locale).stream()
                .map(CategoryNodeRespond::from)
                .toList();
        return ResponseEntity.ok(Result.ok(data));
    }
}
