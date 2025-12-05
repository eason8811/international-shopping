package shopping.international.infrastructure.dao.products.po;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 规格值多语言持久化对象, 对应表 product_spec_value_i18n
 * <p>存储规格值名称的本地化内容</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("product_spec_value_i18n")
public class ProductSpecValueI18nPO {

    /**
     * 规格值ID, 指向 product_spec_value.id
     */
    @Id(keyType = KeyType.None)
    @Column("value_id")
    private Long valueId;

    /**
     * 语言代码, 如 en_US
     */
    @Id(keyType = KeyType.None)
    @Column("locale")
    private String locale;

    /**
     * 规格值名称 (本地化)
     */
    @Column("value_name")
    private String valueName;

    /**
     * 创建时间
     */
    @Column("created_at")
    private LocalDateTime createdAt;
}
