package seungyong.helpmebackend.sse.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import seungyong.helpmebackend.global.filter.AuthenticationFilter;
import seungyong.helpmebackend.global.infrastructure.cookie.CookieUtil;
import seungyong.helpmebackend.sse.application.port.in.SSEPortIn;
import seungyong.helpmebackend.support.config.TestSecurityConfig;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = SSEController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AuthenticationFilter.class
        )
)
@Import(TestSecurityConfig.class)
class SSEControllerTest {
    @Autowired private MockMvc mockMvc;

    @MockitoBean private SSEPortIn ssePortIn;
    @MockitoBean private CookieUtil cookieUtil;

    @Nested
    @DisplayName("subscribe - SSE 구독")
    class Subscribe {
        @Test
        @DisplayName("성공")
        void subscribe_success() throws Exception {
            SseEmitter mockEmitter = new SseEmitter();
            given(ssePortIn.createEmitter()).willReturn(mockEmitter);

            MvcResult mvcResult = mockMvc.perform(get("/api/v1/sse/subscribe")
                            .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.request().asyncStarted()) // 비동기 상태 진입
                    .andReturn();

            // Emitter에 이벤트를 전송하여 비동기 처리를 완료
            mockEmitter.send(SseEmitter.event().name("connected").data("{\"taskId\":\"911fc194-c875-44ef-bc09-1df9c35cdf00\"}"));
            mockEmitter.complete();

            // 이벤트 전송 후, 비동기 처리가 완료되었는지 검증
            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("text/event-stream;charset=UTF-8"));
        }
    }
}