package seungyong.helpmebackend.sse.adapter.out.sse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SSEAdapterTest {
    @Mock private SSERepository sseRepository;

    @InjectMocks private SSEAdapter sseAdapter;

    @Nested
    @DisplayName("createEmitter - SSE 생성")
    class CreateEmitter {
        @Test
        @DisplayName("성공")
        void createEmitter_success() {
            SseEmitter result = sseAdapter.createEmitter();

            assertThat(result).isNotNull();
            verify(sseRepository, times(1)).saveEmitter(anyString(), any(SseEmitter.class));
        }
    }

    @Nested
    @DisplayName("sendCompletion - 작업 완료 이벤트 전송")
    class SendCompletion {
        @Test
        @DisplayName("성공")
        void sendCompletion_success() throws IOException {
            String taskId = "test-id";
            String taskName = "test-task";
            Object data = "test-data";
            SseEmitter mockEmitter = mock(SseEmitter.class);

            given(sseRepository.getEmitter(taskId)).willReturn(mockEmitter);

            boolean result = sseAdapter.sendCompletion(taskId, taskName, data);

            assertThat(result).isTrue();
            verify(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
            verify(mockEmitter).complete();
            verify(sseRepository).removeEmitter(taskId);
        }

        @Test
        @DisplayName("실패 (이미터 존재하지 않음)")
        void sendCompletion_failure_noEmitter() {
            given(sseRepository.getEmitter(anyString())).willReturn(null);

            boolean result = sseAdapter.sendCompletion("invalid-id", "task", "data");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("실패 (전송 중 IOException 발생)")
        void sendCompletion_failure_ioException() throws IOException {
            String taskId = "test-id";
            SseEmitter mockEmitter = mock(SseEmitter.class);

            given(sseRepository.getEmitter(taskId)).willReturn(mockEmitter);
            doThrow(IOException.class).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

            boolean result = sseAdapter.sendCompletion(taskId, "task", "data");

            assertThat(result).isFalse();
            verify(sseRepository).removeEmitter(taskId);
        }
    }

    @Nested
    @DisplayName("deleteEmitter - 이미터 삭제")
    class DeleteEmitter {
        @Test
        @DisplayName("성공")
        void deleteEmitter_success() {
            String taskId = "test-id";

            sseAdapter.deleteEmitter(taskId);

            verify(sseRepository).removeEmitter(taskId);
        }
    }
}