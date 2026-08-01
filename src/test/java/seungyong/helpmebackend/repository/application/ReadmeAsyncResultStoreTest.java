package seungyong.helpmebackend.repository.application;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.global.domain.type.RedisKey;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.repository.application.port.in.result.GeneratedReadmeResult;
import seungyong.helpmebackend.repository.application.port.in.result.ReadmeEvaluationResult;
import seungyong.helpmebackend.repository.domain.exception.RepositoryErrorCode;
import seungyong.helpmebackend.sse.application.port.out.SSEPortOut;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReadmeAsyncResultStoreTest {
    @Mock private RedisPortOut redisPortOut;
    @Mock private SSEPortOut ssePortOut;

    private ReadmeAsyncResultStore store;

    @BeforeEach
    void setUp() {
        store = new ReadmeAsyncResultStore(redisPortOut, ssePortOut);
    }

    @Test
    @DisplayName("평가 fallback을 한 번 반환한 뒤 캐시와 emitter를 정리한다")
    void getEvaluation_consumesFallback() {
        ReadmeEvaluationResult expected = new ReadmeEvaluationResult(4.5F, List.of("good"));
        given(redisPortOut.getObject(anyString(), any(TypeReference.class))).willReturn(expected);

        assertThat(store.getEvaluation("task-id")).isEqualTo(expected);

        verify(ssePortOut).deleteEmitter("task-id");
        verify(redisPortOut).delete(RedisKey.SSE_EMITTER_EVALUATION_DRAFT_KEY.getValue() + "task-id");
    }

    @Test
    @DisplayName("fallback이 없으면 기존 NOT_FOUND 오류를 반환한다")
    void getGeneratedReadme_notFound() {
        given(redisPortOut.getObject(anyString(), any(TypeReference.class))).willReturn(null);

        assertThatThrownBy(() -> store.getGeneratedReadme("task-id"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", RepositoryErrorCode.FALLBACK_NOT_FOUND);

        verify(ssePortOut, never()).deleteEmitter(anyString());
        verify(redisPortOut, never()).delete(anyString());
    }

    @Test
    @DisplayName("SSE 전송에 성공하면 평가 결과를 fallback으로 중복 저장하지 않는다")
    void publishEvaluation_sseSuccess() {
        ReadmeEvaluationResult result = new ReadmeEvaluationResult(4.0F, List.of());
        given(ssePortOut.sendCompletion("task-id", "task-name", result)).willReturn(true);

        store.publishEvaluation("task-id", "task-name", result);

        verify(redisPortOut, never()).setObjectIfAbsent(anyString(), any(), any(Instant.class));
    }

    @Test
    @DisplayName("SSE 전송에 실패하면 생성 결과를 fallback으로 저장한다")
    void publishGeneratedReadme_sseFailure() {
        GeneratedReadmeResult result = new GeneratedReadmeResult(List.of());
        given(ssePortOut.sendCompletion("task-id", "task-name", result)).willReturn(false);

        store.publishGeneratedReadme("task-id", "task-name", result);

        verify(redisPortOut).setObjectIfAbsent(
                eq(RedisKey.SSE_EMITTER_GENERATION_KEY.getValue() + "task-id"),
                eq(result),
                any(Instant.class)
        );
    }

    @Test
    @DisplayName("알 수 없는 비동기 오류는 내부 서버 오류 이벤트로 축약한다")
    void publishError_hidesUnexpectedException() {
        store.publishError("task-id", "task-name", new IllegalStateException("secret"));

        verify(ssePortOut).sendError("task-id", "task-name", GlobalErrorCode.INTERNAL_SERVER_ERROR);
    }
}
