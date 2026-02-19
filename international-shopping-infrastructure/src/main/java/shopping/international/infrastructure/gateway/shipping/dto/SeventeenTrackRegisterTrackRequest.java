package shopping.international.infrastructure.gateway.shipping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

/**
 * 17Track 注册运单请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeventeenTrackRegisterTrackRequest {

    /**
     * 追踪号
     */
    private String number;
    /**
     * 承运商编码, 17Track 支持的 carrier key
     */
    @Nullable
    private String carrier;
}
