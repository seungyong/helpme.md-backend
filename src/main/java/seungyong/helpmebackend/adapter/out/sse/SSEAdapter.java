package seungyong.helpmebackend.adapter.out.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.domain.exception.SseErrorCode;
import seungyong.helpmebackend.usecase.port.out.sse.SSEPortOut;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SSEAdapter implements SSEPortOut {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final static Long DEFAULT_TIMEOUT = 20L * 60 * 1000L; // 20 minutes

    @Override
    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        String taskId = UUID.randomUUID().toString();

        emitter.onCompletion(() -> emitters.remove(taskId));
        emitter.onTimeout(() -> emitters.remove(taskId));
        emitter.onError((e) -> emitters.remove(taskId));

        emitters.put(taskId, emitter);

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("connected")
                            .data(Map.of("taskId", taskId))
            );
        } catch (Exception e) {
            log.error("Error creating SSE emitter for taskId = {}", taskId, e);
            emitters.remove(taskId);
            throw new CustomException(SseErrorCode.CONNECTION_FAILED);
        }

        return emitter;
    }

    @Override
    public boolean sendCompletion(String taskId, String taskName, Object data) {
        SseEmitter emitter = emitters.get(taskId);

        if (emitter == null) {
            log.warn("No emitter found for taskName = {}, taskId = {}", taskName, taskId);
            return false;
        }

        try {
            emitter.send(SseEmitter.event().name(taskName).data(data, MediaType.APPLICATION_JSON));
            emitter.complete();
            emitters.remove(taskId);
            return true;
        } catch (IOException e) {
            log.error("Error sending completion event to taskName = {}, taskId = {}", taskName, taskId, e);
            emitters.remove(taskId);
            return false;
        }
    }

    @Override
    public void deleteEmitter(String taskId) {
        emitters.remove(taskId);
    }
}
