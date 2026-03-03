package seungyong.helpmebackend.sse.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import seungyong.helpmebackend.sse.application.port.in.SSEPortIn;
import seungyong.helpmebackend.sse.application.port.out.SSEPortOut;

@Service
@RequiredArgsConstructor
public class SSEService implements SSEPortIn {
    private final SSEPortOut ssePortOut;

    @Override
    public SseEmitter createEmitter() {
        return ssePortOut.createEmitter();
    }
}
