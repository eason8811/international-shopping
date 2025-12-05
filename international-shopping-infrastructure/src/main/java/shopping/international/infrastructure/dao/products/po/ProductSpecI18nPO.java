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
 * 规格类别多语言持久化对象, 对应表 product_spec_i18n
 * <p>存储规格类别名称的本地化内容</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("product_spec_i18n")
public class ProductSpecI18nPO {

    /**
     * 规格类别ID, 指向 product_spec.id
     */
    @Id(keyType = KeyType.None)
    @Column("spec_id")
    private Long specId;

    /**
     * 语言代码, 如 en_US
     */
    @Id(keyType = KeyType.None)
    @Column("locale")
    private String locale;

    /**
     * 规格类别名称 (本地化)
     */
    @Column("spec_name")
    private String specName;

    /**
     * 创建时间
     */
    @Column("created_at")
    private LocalDateTime createdAt;
}
