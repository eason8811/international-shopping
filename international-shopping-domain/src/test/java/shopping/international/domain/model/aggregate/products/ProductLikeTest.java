package shopping.international.domain.model.aggregate.products;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import shopping.international.domain.support.LoggingTestWatcher;
import shopping.international.types.exceptions.IllegalParamException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(LoggingTestWatcher.class)
class ProductLikeTest {

    @Test
    void likeShouldCreateSnapshot() {
        ProductLike like = ProductLike.like(1L, 2L);

        assertEquals(1L, like.getUserId());
        assertEquals(2L, like.getProductId());
        assertNotNull(like.getLikedAt());
    }

    @Test
    void refreshShouldOverwriteTimestamp() {
        ProductLike like = ProductLike.reconstitute(1L, 2L, LocalDateTime.now().minusDays(1));
        LocalDateTime now = LocalDateTime.now();

        like.refreshLikedAt(now);

        assertEquals(now, like.getLikedAt());
        assertThrows(IllegalParamException.class, () -> like.refreshLikedAt(null));
    }
}
