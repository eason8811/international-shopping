package shopping.international.infrastructure.dao.products;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import shopping.international.infrastructure.dao.products.po.ProductPO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mapper: product
 */
@Mapper
public interface ProductMapper extends BaseMapper<ProductPO> {

    @Select("""
            <script>
            SELECT p.*
            FROM product p
            WHERE p.status = 'ON_SALE'
              AND EXISTS (SELECT 1 FROM product_sku s WHERE s.product_id = p.id AND s.status = 'ENABLED')
              <if test="categoryId != null">
                AND p.category_id = #{categoryId}
              </if>
              <if test="keyword != null and keyword != ''">
                AND (
                    p.title LIKE CONCAT('%', #{keyword}, '%')
                    OR p.subtitle LIKE CONCAT('%', #{keyword}, '%')
                    <if test="locale != null and locale != ''">
                      OR EXISTS (
                        SELECT 1 FROM product_i18n pi
                        WHERE pi.product_id = p.id
                          AND pi.locale = #{locale}
                          AND (pi.title LIKE CONCAT('%', #{keyword}, '%') OR pi.subtitle LIKE CONCAT('%', #{keyword}, '%'))
                      )
                    </if>
                )
              </if>
              <if test="tags != null and tags.size() > 0">
                AND (
                  <foreach collection="tags" item="tag" separator=" OR ">
                    JSON_CONTAINS(p.tags, CONCAT('\"', #{tag}, '\"'))
                    <if test="locale != null and locale != ''">
                      OR EXISTS (
                        SELECT 1 FROM product_i18n pi
                        WHERE pi.product_id = p.id
                          AND pi.locale = #{locale}
                          AND JSON_CONTAINS(pi.tags, CONCAT('\"', #{tag}, '\"'))
                      )
                    </if>
                  </foreach>
                )
              </if>
              <if test="currency != null and currency != ''">
                AND EXISTS (
                  SELECT 1
                  FROM product_sku s
                  JOIN product_price pp ON pp.sku_id = s.id
                  WHERE s.product_id = p.id
                    AND s.status = 'ENABLED'
                    AND pp.currency = #{currency}
                    AND pp.is_active = 1
                    <if test="priceMin != null">AND COALESCE(pp.sale_price, pp.list_price) &gt;= #{priceMin}</if>
                    <if test="priceMax != null">AND COALESCE(pp.sale_price, pp.list_price) &lt;= #{priceMax}</if>
                )
              </if>
            <choose>
              <when test="sortBy == 'SALES_DESC'">
                ORDER BY p.sale_count DESC, p.updated_at DESC
              </when>
              <when test="sortBy == 'PRICE_ASC' and currency != null and currency != ''">
                ORDER BY (
                  SELECT MIN(COALESCE(pp.sale_price, pp.list_price))
                  FROM product_sku s
                  JOIN product_price pp ON pp.sku_id = s.id
                  WHERE s.product_id = p.id AND s.status = 'ENABLED' AND pp.currency = #{currency} AND pp.is_active = 1
                ) ASC, p.updated_at DESC
              </when>
              <when test="sortBy == 'PRICE_DESC' and currency != null and currency != ''">
                ORDER BY (
                  SELECT MAX(COALESCE(pp.sale_price, pp.list_price))
                  FROM product_sku s
                  JOIN product_price pp ON pp.sku_id = s.id
                  WHERE s.product_id = p.id AND s.status = 'ENABLED' AND pp.currency = #{currency} AND pp.is_active = 1
                ) DESC, p.updated_at DESC
              </when>
              <otherwise>
                ORDER BY p.updated_at DESC
              </otherwise>
            </choose>
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
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

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM product p
            WHERE p.status = 'ON_SALE'
              AND EXISTS (SELECT 1 FROM product_sku s WHERE s.product_id = p.id AND s.status = 'ENABLED')
              <if test="categoryId != null">
                AND p.category_id = #{categoryId}
              </if>
              <if test="keyword != null and keyword != ''">
                AND (
                    p.title LIKE CONCAT('%', #{keyword}, '%')
                    OR p.subtitle LIKE CONCAT('%', #{keyword}, '%')
                    <if test="locale != null and locale != ''">
                      OR EXISTS (
                        SELECT 1 FROM product_i18n pi
                        WHERE pi.product_id = p.id
                          AND pi.locale = #{locale}
                          AND (pi.title LIKE CONCAT('%', #{keyword}, '%') OR pi.subtitle LIKE CONCAT('%', #{keyword}, '%'))
                      )
                    </if>
                )
              </if>
              <if test="tags != null and tags.size() > 0">
                AND (
                  <foreach collection="tags" item="tag" separator=" OR ">
                    JSON_CONTAINS(p.tags, CONCAT('\"', #{tag}, '\"'))
                    <if test="locale != null and locale != ''">
                      OR EXISTS (
                        SELECT 1 FROM product_i18n pi
                        WHERE pi.product_id = p.id
                          AND pi.locale = #{locale}
                          AND JSON_CONTAINS(pi.tags, CONCAT('\"', #{tag}, '\"'))
                      )
                    </if>
                  </foreach>
                )
              </if>
              <if test="currency != null and currency != ''">
                AND EXISTS (
                  SELECT 1
                  FROM product_sku s
                  JOIN product_price pp ON pp.sku_id = s.id
                  WHERE s.product_id = p.id
                    AND s.status = 'ENABLED'
                    AND pp.currency = #{currency}
                    AND pp.is_active = 1
                    <if test="priceMin != null">AND COALESCE(pp.sale_price, pp.list_price) &gt;= #{priceMin}</if>
                    <if test="priceMax != null">AND COALESCE(pp.sale_price, pp.list_price) &lt;= #{priceMax}</if>
                )
              </if>
            </script>
            """)
    long countOnSale(@Param("categoryId") Long categoryId,
                     @Param("keyword") String keyword,
                     @Param("tags") List<String> tags,
                     @Param("locale") String locale,
                     @Param("currency") String currency,
                     @Param("priceMin") BigDecimal priceMin,
                     @Param("priceMax") BigDecimal priceMax);

    @Select("""
            SELECT p.*
            FROM product p
            WHERE p.slug = #{slug}
              AND p.status = 'ON_SALE'
              AND EXISTS (SELECT 1 FROM product_sku s WHERE s.product_id = p.id AND s.status = 'ENABLED')
            LIMIT 1
            """)
    ProductPO selectOnSaleBySlug(@Param("slug") String slug);

    @Select("""
            SELECT p.*
            FROM product p
            JOIN product_i18n pi ON pi.product_id = p.id
            WHERE p.status = 'ON_SALE'
              AND pi.locale = #{locale}
              AND pi.slug = #{slug}
              AND EXISTS (SELECT 1 FROM product_sku s WHERE s.product_id = p.id AND s.status = 'ENABLED')
            LIMIT 1
            """)
    ProductPO selectOnSaleByLocalizedSlug(@Param("slug") String slug, @Param("locale") String locale);
}
