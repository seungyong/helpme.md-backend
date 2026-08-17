package seungyong.helpmebackend.webhook.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.filter.AuthenticationFilter;
import seungyong.helpmebackend.global.infrastructure.cookie.CookieUtil;
import seungyong.helpmebackend.support.config.TestSecurityConfig;
import seungyong.helpmebackend.webhook.application.port.in.WebhookPortIn;
import seungyong.helpmebackend.webhook.application.port.in.result.WebhookReceiptResult;
import seungyong.helpmebackend.webhook.application.port.in.result.WebhookTestResult;

import java.time.OffsetDateTime;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = WebhookController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AuthenticationFilter.class
        )
)
@Import(TestSecurityConfig.class)
class WebhookControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private WebhookPortIn webhookPortIn;
    @MockitoBean private CookieUtil cookieUtil;

    @Test
    void webhookReceiptReturnsAcceptedContract() throws Exception {
        byte[] body = "{\"repository\":{\"id\":778899}}".getBytes();
        given(webhookPortIn.receive("sha256=valid", "push", "delivery-1", body))
                .willReturn(new WebhookReceiptResult("accepted", "delivery-1", 1));

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-Hub-Signature-256", "sha256=valid")
                        .header("X-GitHub-Event", "push")
                        .header("X-GitHub-Delivery", "delivery-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.projectCount").value(1));
    }

    @Test
    void webhookTestStartAndStatusUseSameResource() throws Exception {
        given(webhookPortIn.startTest(1L, 101L)).willReturn(
                new WebhookTestResult("test-1", "queued", null, null, null, null)
        );
        OffsetDateTime now = OffsetDateTime.parse("2026-08-17T00:00:00Z");
        given(webhookPortIn.getTest(1L, 101L, "test-1")).willReturn(
                new WebhookTestResult("test-1", "succeeded", "delivery-1", now, now, null)
        );

        mockMvc.perform(post("/api/v1/projects/101/webhook-tests").with(user()))
                .andExpect(status().isAccepted())
                .andExpect(header().string(
                        "Location", "/api/v1/projects/101/webhook-tests/test-1"
                ))
                .andExpect(jsonPath("$.deliveryId").value((Object) null))
                .andExpect(jsonPath("$.status").value("queued"));

        mockMvc.perform(get("/api/v1/projects/101/webhook-tests/test-1").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("succeeded"))
                .andExpect(jsonPath("$.deliveryId").value("delivery-1"))
                .andExpect(jsonPath("$.error").value((Object) null));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor user() {
        return SecurityMockMvcRequestPostProcessors.user(
                new CustomUserDetails(1L, "test-user")
        );
    }
}
