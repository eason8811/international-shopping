package shopping.international.infrastructure.dao.products.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("product_spec_value_i18n")
public class ProductSpecValueI18nPO {

    /**
     * 规格值ID, 指向 product_spec_value.id
     */
    @TableId(value = "value_id", type = IdType.INPUT)
    private Long valueId;

    /**
     * 语言代码, 如 en_US
     */
    @TableField("locale")
    private String locale;

    /**
     * 规格值名称 (本地化)
     */
    @TableField("value_name")
    private String valueName;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
