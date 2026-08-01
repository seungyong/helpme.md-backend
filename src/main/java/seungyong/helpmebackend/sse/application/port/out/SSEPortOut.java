package seungyong.helpmebackend.sse.application.port.out;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import seungyong.helpmebackend.global.exception.ErrorCode;

public interface SSEPortOut {
    SseEmitter createEmitter();
    boolean sendCompletion(String taskId, String taskName, Object data);
    boolean sendError(String taskId, String taskName, ErrorCode errorCode);
    void deleteEmitter(String taskId);
}
