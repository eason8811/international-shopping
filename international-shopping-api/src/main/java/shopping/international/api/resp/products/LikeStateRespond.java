package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.vo.products.LikeState;

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

    /**
     * 根据点赞状态创建一个 <code>LikeStateRespond</code> 对象
     *
     * @param state 代表点赞状态的 <code>LikeState</code> 对象, 如果为 null, 则表示没有点赞信息
     * @return 返回一个 <code>LikeStateRespond</code> 对象, 其中包含是否已点赞以及点赞时间的信息
     */
    public static LikeStateRespond from(LikeState state) {
        if (state == null)
            return new LikeStateRespond(false, null);
        return new LikeStateRespond(state.liked(), state.likedAt());
    }
}
