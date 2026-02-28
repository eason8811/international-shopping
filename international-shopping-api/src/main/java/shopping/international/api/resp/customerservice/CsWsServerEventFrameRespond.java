package shopping.international.api.resp.customerservice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.enums.customerservice.WsFrameType;

/**
 * WebSocket 服务端事件帧响应对象
 *
 * @param <T> 事件响应的数据负载类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsWsServerEventFrameRespond<T> {
    /**
     * 帧类型，固定为 event
     */
    @NotNull
    private WsFrameType type;
    /**
     * 事件包裹
     */
    @NotNull
    private CsWsEventEnvelopeRespond<T> event;
}
