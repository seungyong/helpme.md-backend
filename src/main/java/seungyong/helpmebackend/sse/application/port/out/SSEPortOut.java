package seungyong.helpmebackend.sse.application.port.out;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SSEPortOut {
    SseEmitter createEmitter();
    boolean sendCompletion(String taskId, String taskName, Object data);
    void deleteEmitter(String taskId);
}
