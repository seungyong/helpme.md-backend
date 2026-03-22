package seungyong.helpmebackend.sse.adapter.out.sse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SSERepositoryTest {
    private SSERepository sseRepository;

    @BeforeEach
    void setUp() {
        sseRepository = new SSERepository();
    }

    @Nested
    @DisplayName("saveEmitter - SSE 저장")
    class SaveEmitter {
        @Test
        @DisplayName("성공")
        void saveEmitter_success() {
            String taskId = "test-task-id";
            SseEmitter mockEmitter = mock(SseEmitter.class);

            sseRepository.saveEmitter(taskId, mockEmitter);

            SseEmitter savedEmitter = sseRepository.getEmitter(taskId);
            assertThat(savedEmitter).isEqualTo(mockEmitter);
            verify(mockEmitter).onCompletion(any());
            verify(mockEmitter).onTimeout(any());
            verify(mockEmitter).onError(any());
        }
    }

    @Nested
    @DisplayName("getEmitter - SSE 조회")
    class GetEmitter {
        @Test
        @DisplayName("성공")
        void getEmitter_success() {
            String taskId = "test-task-id";
            SseEmitter mockEmitter = mock(SseEmitter.class);
            sseRepository.saveEmitter(taskId, mockEmitter);

            SseEmitter result = sseRepository.getEmitter(taskId);

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(mockEmitter);
        }

        @Test
        @DisplayName("실패 (존재하지 않는 ID)")
        void getEmitter_failure_notFound() {
            SseEmitter result = sseRepository.getEmitter("non-existent-id");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("removeEmitter - SSE 제거")
    class RemoveEmitter {
        @Test
        @DisplayName("성공")
        void removeEmitter_success() {
            String taskId = "test-task-id";
            SseEmitter mockEmitter = mock(SseEmitter.class);
            sseRepository.saveEmitter(taskId, mockEmitter);

            sseRepository.removeEmitter(taskId);

            assertThat(sseRepository.getEmitter(taskId)).isNull();
        }
    }
}