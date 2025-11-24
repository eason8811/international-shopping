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
    @TableId("spec_id")
    private Long specId;
    @TableField("locale")
    private String locale;
    @TableField("spec_name")
    private String specName;
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
