package seungyong.helpmebackend.repository.application;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.global.domain.type.RedisKey;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.ErrorCode;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.repository.application.port.in.result.GeneratedReadmeResult;
import seungyong.helpmebackend.repository.application.port.in.result.ReadmeEvaluationResult;
import seungyong.helpmebackend.repository.domain.exception.RepositoryErrorCode;
import seungyong.helpmebackend.sse.application.port.out.SSEPortOut;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
class ReadmeAsyncResultStore {
    private final RedisPortOut redisPortOut;
    private final SSEPortOut ssePortOut;

    public ReadmeEvaluationResult getEvaluation(String taskId) {
        return getFallbackResult(
                RedisKey.SSE_EMITTER_EVALUATION_DRAFT_KEY.getValue() + taskId,
                taskId,
                new TypeReference<>() {
                }
        );
    }

    public GeneratedReadmeResult getGeneratedReadme(String taskId) {
        return getFallbackResult(
                RedisKey.SSE_EMITTER_GENERATION_KEY.getValue() + taskId,
                taskId,
                new TypeReference<>() {
                }
        );
    }

    public void publishEvaluation(String taskId, String taskName, ReadmeEvaluationResult result) {
        publish(
                RedisKey.SSE_EMITTER_EVALUATION_DRAFT_KEY.getValue() + taskId,
                taskId,
                taskName,
                result
        );
    }

    public void publishGeneratedReadme(String taskId, String taskName, GeneratedReadmeResult result) {
        publish(
                RedisKey.SSE_EMITTER_GENERATION_KEY.getValue() + taskId,
                taskId,
                taskName,
                result
        );
    }

    public void publishError(String taskId, String taskName, Exception exception) {
        log.warn(
                "README SSE task failed: taskName={}, taskId={}, exceptionType={}",
                taskName,
                taskId,
                exception.getClass().getSimpleName()
        );

        ErrorCode errorCode = exception instanceof CustomException customException
                ? customException.getErrorCode()
                : GlobalErrorCode.INTERNAL_SERVER_ERROR;
        ssePortOut.sendError(taskId, taskName, errorCode);
    }

    private <T> T getFallbackResult(
            String key,
            String taskId,
            TypeReference<T> typeReference
    ) {
        T cached = redisPortOut.getObject(key, typeReference);
        if (cached == null) {
            throw new CustomException(RepositoryErrorCode.FALLBACK_NOT_FOUND);
        }

        ssePortOut.deleteEmitter(taskId);
        redisPortOut.delete(key);
        return cached;
    }

    private void publish(String key, String taskId, String taskName, Object data) {
        if (ssePortOut.sendCompletion(taskId, taskName, data)) {
            return;
        }

        redisPortOut.setObjectIfAbsent(
                key,
                data,
                Instant.now().plus(1, ChronoUnit.HOURS)
        );
    }
}
