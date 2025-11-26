package shopping.international.domain.model.vo.products;

import java.time.LocalDateTime;

/**
 * 点赞状态
 *
 * @param liked   是否已点赞
 * @param likedAt 点赞时间
 */
public record LikeState(boolean liked, LocalDateTime likedAt) {

    /**
     * 创建一个表示已点赞状态的 <code>LikeState</code> 对象
     *
     * @param likedAt 点赞时间, 用于记录用户执行点赞操作的具体时间
     * @return 返回一个 <code>LikeState</code> 对象, 其中 <code>liked</code> 属性为 true, 表示已点赞, 并携带了具体的点赞时间
     */
    public static LikeState liked(LocalDateTime likedAt) {
        return new LikeState(true, likedAt);
    }

    /**
     * 创建一个表示未点赞状态的 <code>LikeState</code> 对象
     *
     * @return 返回一个 <code>LikeState</code> 对象, 其中 <code>liked</code> 属性为 false, 表示未点赞, 且 <code>likedAt</code> 为 null
     */
    public static LikeState unliked() {
        return new LikeState(false, null);
    }
}
