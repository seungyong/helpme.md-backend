package seungyong.helpmebackend.sse;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.global.config.SecurityConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(SecurityConfig.class)
public class SSEIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("subscribe - SSE 구독")
    class Subscribe {
        @Test
        @DisplayName("성공")
        void subscribe_success() throws Exception {
            // when: 실제 애플리케이션 흐름을 타고 API를 호출합니다.
            MvcResult mvcResult = mockMvc.perform(get("/api/v1/sse/subscribe")
                            .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted())
                    .andReturn();

            String contentType = mvcResult.getResponse().getContentType();
            String contentAsString = mvcResult.getResponse().getContentAsString();

            log.info("Received SSE response: contentType={}, contentAsString={}", contentType, contentAsString);

            assertThat(contentType).isNotNull();
            assertThat(contentType).contains("text/event-stream");

            assertThat(contentAsString)
                    .contains("event:connected")
                    .contains("data:{\"taskId\":\"");
        }
    }
}
