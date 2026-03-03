package seungyong.helpmebackend.sse.application.port.in;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SSEPortIn {
    SseEmitter createEmitter();
}
