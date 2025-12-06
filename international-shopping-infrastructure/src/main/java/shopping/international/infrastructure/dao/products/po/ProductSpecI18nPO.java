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
 * 规格类别多语言持久化对象, 对应表 product_spec_i18n
 * <p>存储规格类别名称的本地化内容</p>
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
    @TableId(value = "spec_id", type = IdType.INPUT)
    private Long specId;

    /**
     * 语言代码, 如 en_US
     */
    @TableField("locale")
    private String locale;

    /**
     * 规格类别名称 (本地化)
     */
    @TableField("spec_name")
    private String specName;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
