package shopping.international.app.products;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import shopping.international.domain.model.aggregate.products.Category;
import shopping.international.domain.model.aggregate.products.Product;
import shopping.international.domain.model.aggregate.products.Sku;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.vo.products.CategoryI18n;
import shopping.international.domain.model.vo.products.ProductImage;
import shopping.international.domain.model.vo.products.ProductPrice;
import shopping.international.domain.service.products.ICategoryService;
import shopping.international.domain.service.products.IProductService;
import shopping.international.domain.service.products.ISkuService;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 统一的 API 集成测试基类:
 * <ul>
 *   <li>使用 application-dev 中的数据源配置</li>
 *   <li>若当前库不存在表, 自动执行 dev SQL 脚本初始化结构</li>
 *   <li>每个用例前清空商品相关表, 保持测试隔离</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ProductApiIntegrationTestBase {

    protected static final String API_PREFIX = "/api/v1";

    private static final AtomicBoolean SCHEMA_INITIALIZED = new AtomicBoolean(false);

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected DataSource dataSource;
    @Autowired
    protected ICategoryService categoryService;
    @Autowired
    protected IProductService productService;
    @Autowired
    protected ISkuService skuService;

    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerMysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.profiles.active", () -> "dev");
    }

    @BeforeAll
    void loadSchema() throws Exception {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        if (SCHEMA_INITIALIZED.compareAndSet(false, true)) {
            initializeSchemaIfMissing();
        }
    }

    @BeforeEach
    void resetTables() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
        // 仅清理商品与分类相关表, 避免跨领域的脏数据干扰
        List<String> tables = List.of(
                "product_like",
                "product_sku_spec",
                "product_sku_image",
                "product_price",
                "product_sku",
                "product_spec_value",
                "product_spec_value_i18n",
                "product_spec",
                "product_spec_i18n",
                "product_image",
                "product_i18n",
                "product",
                "product_category_i18n",
                "product_category"
        );
        tables.forEach(table -> jdbcTemplate.execute("TRUNCATE TABLE " + table));
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
    }

    /**
     * 如果当前库缺少核心表, 执行完整的 dev SQL 脚本初始化
     */
    private void initializeSchemaIfMissing() throws Exception {
        boolean schemaMissing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'product'",
                Integer.class) == 0;
        if (!schemaMissing)
            return;
        Path schemaPath = Path.of("../dev-ops/dev/international-shopping.sql").toAbsolutePath();
        if (!Files.exists(schemaPath)) {
            throw new IllegalStateException("Schema script not found: " + schemaPath);
        }
        try (Connection connection = DataSourceUtils.getConnection(dataSource)) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource(schemaPath));
        }
    }

    /**
     * 创建一个启用的根分类并返回
     */
    protected Category seedCategory(String name, String slug, String locale) {
        CategoryI18n i18n = CategoryI18n.of(locale, name + " " + locale, slug + "-" + locale, null);
        return categoryService.create(name, slug, null, 1, true, List.of(i18n));
    }

    /**
     * 创建一个子分类, 复用父级的 locale 覆盖
     */
    protected Category seedChildCategory(Category parent, String name, String slug, String locale) {
        CategoryI18n i18n = CategoryI18n.of(locale, name + " " + locale, slug + "-" + locale, null);
        return categoryService.create(name, slug, parent.getId(), parent.getSortOrder() + 1, true, List.of(i18n));
    }

    /**
     * 创建一个包含默认 SKU 的上架商品, 便于用户侧查询测试
     */
    protected SeedProduct seedOnSaleProduct(String slug, String locale, String currency) {
        return seedOnSaleProduct(slug, locale, currency, List.of("tag1", "tag2"),
                new BigDecimal("99.99"), new BigDecimal("79.99"));
    }

    protected SeedProduct seedOnSaleProduct(String slug, String locale, String currency,
                                            List<String> tags, BigDecimal listPrice, BigDecimal salePrice) {
        return seedProduct(slug, locale, currency, ProductStatus.ON_SALE, tags, listPrice, salePrice);
    }

    protected SeedProduct seedProduct(String slug, String locale, String currency, ProductStatus status,
                                      List<String> tags, BigDecimal listPrice, BigDecimal salePrice) {
        String categorySlug = slug + "-cat";
        Category category = seedCategory("Electronics " + slug, categorySlug, locale);
        Product product = productService.createBasic(
                slug,
                "Test " + slug,
                "Sub " + slug,
                "Desc for " + slug,
                category.getId(),
                "BrandX",
                "https://img/" + slug + ".jpg",
                SkuType.SINGLE,
                status,
                tags
        );
        ProductPrice price = ProductPrice.of(currency, listPrice, salePrice, true);
        ProductImage skuImage = ProductImage.of("https://img/" + slug + "/sku.jpg", true, 1);
        Sku sku = skuService.create(
                product.getId(),
                slug + "-sku",
                50,
                new BigDecimal("1.2"),
                SkuStatus.ENABLED,
                true,
                "BAR-" + slug,
                List.of(price),
                List.of(),
                List.of(skuImage)
        );
        return new SeedProduct(category, product, sku);
    }

    /**
     * 简单的种子返回结构, 方便测试断言时取 ID/slug
     */
    protected record SeedProduct(Category category, Product product, Sku sku) {
    }
}
