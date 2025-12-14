package shopping.international.api.resp.products;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 点赞状态响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeStateRespond {
    /**
     * 是否已点赞
     */
    private Boolean liked;
    /**
     * 点赞时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime likedAt;
}
