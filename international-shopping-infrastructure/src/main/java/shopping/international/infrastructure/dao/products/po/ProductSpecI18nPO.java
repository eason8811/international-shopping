package shopping.international.infrastructure.dao.products.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 持久化对象: product_spec_i18n
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_spec_i18n")
public class ProductSpecI18nPO {
    /**
     * 规格类别ID, 指向 product_spec.id
     */
    @TableId("spec_id")
    private Long specId;
    /**
     * 语言代码, 指向 locale.code
     */
    @TableField("locale")
    private String locale;
    /**
     * 规格类别名(本地化)
     */
    @TableField("spec_name")
    private String specName;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
