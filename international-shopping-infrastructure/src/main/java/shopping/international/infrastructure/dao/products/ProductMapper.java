package shopping.international.infrastructure.dao.products;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.products.po.ProductPO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mapper: product
 */
@Mapper
public interface ProductMapper extends BaseMapper<ProductPO> {

    List<ProductPO> selectOnSalePage(@Param("offset") int offset,
                                     @Param("limit") int limit,
                                     @Param("categoryId") Long categoryId,
                                     @Param("keyword") String keyword,
                                     @Param("tags") List<String> tags,
                                     @Param("locale") String locale,
                                     @Param("currency") String currency,
                                     @Param("priceMin") BigDecimal priceMin,
                                     @Param("priceMax") BigDecimal priceMax,
                                     @Param("sortBy") String sortBy);

    long countOnSale(@Param("categoryId") Long categoryId,
                     @Param("keyword") String keyword,
                     @Param("tags") List<String> tags,
                     @Param("locale") String locale,
                     @Param("currency") String currency,
                     @Param("priceMin") BigDecimal priceMin,
                     @Param("priceMax") BigDecimal priceMax);

    ProductPO selectOnSaleBySlug(@Param("slug") String slug);

    ProductPO selectOnSaleByLocalizedSlug(@Param("slug") String slug, @Param("locale") String locale);

    List<ProductPO> selectAdminPage(@Param("status") String status,
                                    @Param("skuType") String skuType,
                                    @Param("categoryId") Long categoryId,
                                    @Param("keyword") String keyword,
                                    @Param("tag") String tag,
                                    @Param("includeDeleted") boolean includeDeleted,
                                    @Param("limit") int limit,
                                    @Param("offset") int offset);

    long countAdminPage(@Param("status") String status,
                        @Param("skuType") String skuType,
                        @Param("categoryId") Long categoryId,
                        @Param("keyword") String keyword,
                        @Param("tag") String tag,
                        @Param("includeDeleted") boolean includeDeleted);
}
