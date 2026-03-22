package seungyong.helpmebackend.sse.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import seungyong.helpmebackend.sse.application.port.out.SSEPortOut;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class SSEServiceTest {
    @Mock private SSEPortOut ssePortOut;
    @InjectMocks private SSEService sseService;

    @Test
    @DisplayName("createEmitter - SSE 생성")
    void createEmitter() {
        SseEmitter emitter = Mockito.mock(SseEmitter.class);
        given(ssePortOut.createEmitter()).willReturn(emitter);

        SseEmitter result = sseService.createEmitter();

        assertThat(result).isNotNull().isEqualTo(emitter);
    }
}
