package seungyong.helpmebackend.usecase.service.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import seungyong.helpmebackend.usecase.port.in.sse.SSEPortIn;
import seungyong.helpmebackend.usecase.port.out.sse.SSEPortOut;

@Service
@RequiredArgsConstructor
public class SSEService implements SSEPortIn {
    private final SSEPortOut ssePortOut;

    @Override
    public SseEmitter createEmitter() {
        return ssePortOut.createEmitter();
    }
}
