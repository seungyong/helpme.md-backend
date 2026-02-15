package seungyong.helpmebackend.usecase.port.in.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SSEPortIn {
    SseEmitter createEmitter();
}
