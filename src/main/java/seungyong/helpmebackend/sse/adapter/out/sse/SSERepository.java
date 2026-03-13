package seungyong.helpmebackend.sse.adapter.out.sse;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import seungyong.helpmebackend.sse.application.port.out.SSERepositoryPortOut;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
class SSERepository implements SSERepositoryPortOut {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Override
    public void saveEmitter(String taskId, SseEmitter emitter) {
        emitter.onCompletion(() -> emitters.remove(taskId));
        emitter.onTimeout(() -> emitters.remove(taskId));
        emitter.onError((e) -> emitters.remove(taskId));

        emitters.put(taskId, emitter);
    }

    @Override
    public SseEmitter getEmitter(String taskId) {
        return emitters.get(taskId);
    }

    @Override
    public void removeEmitter(String taskId) {
        emitters.remove(taskId);
    }
}
