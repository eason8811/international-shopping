package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 点赞状态响应
 */
@Data
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
    private LocalDateTime likedAt;
}
