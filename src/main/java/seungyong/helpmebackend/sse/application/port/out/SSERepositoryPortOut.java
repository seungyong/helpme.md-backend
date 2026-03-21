package seungyong.helpmebackend.sse.application.port.out;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SSERepositoryPortOut {
    void saveEmitter(String taskId, SseEmitter emitter);
    SseEmitter getEmitter(String taskId);
    void removeEmitter(String taskId);
}
