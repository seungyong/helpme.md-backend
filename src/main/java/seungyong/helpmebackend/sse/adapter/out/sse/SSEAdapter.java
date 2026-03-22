package seungyong.helpmebackend.sse.adapter.out.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.sse.application.port.out.SSEPortOut;
import seungyong.helpmebackend.sse.domain.exception.SseErrorCode;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SSEAdapter implements SSEPortOut {
    private final SSERepository sseRepository;
    private final static Long DEFAULT_TIMEOUT = 20L * 60 * 1000L; // 20 minutes

    @Override
    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        String taskId = UUID.randomUUID().toString();
        sseRepository.saveEmitter(taskId, emitter);

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("connected")
                            .data(Map.of("taskId", taskId))
            );
        } catch (Exception e) {
            log.error("Error creating SSE emitter for taskId = {}", taskId, e);
            sseRepository.removeEmitter(taskId);
            throw new CustomException(SseErrorCode.CONNECTION_FAILED);
        }

        return emitter;
    }

    @Override
    public boolean sendCompletion(String taskId, String taskName, Object data) {
        SseEmitter emitter = sseRepository.getEmitter(taskId);

        if (emitter == null) {
            log.warn("No emitter found for taskName = {}, taskId = {}", taskName, taskId);
            return false;
        }

        try {
            emitter.send(SseEmitter.event().name(taskName).data(data, MediaType.APPLICATION_JSON));
            emitter.complete();
            sseRepository.removeEmitter(taskId);
            return true;
        } catch (IOException e) {
            log.error("Error sending completion event to taskName = {}, taskId = {}", taskName, taskId, e);
            sseRepository.removeEmitter(taskId);
            return false;
        }
    }

    @Override
    public void deleteEmitter(String taskId) {
        sseRepository.removeEmitter(taskId);
    }
}
