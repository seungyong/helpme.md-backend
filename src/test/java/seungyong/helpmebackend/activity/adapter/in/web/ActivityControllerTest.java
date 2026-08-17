package seungyong.helpmebackend.activity.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import seungyong.helpmebackend.activity.application.port.in.ActivityPortIn;
import seungyong.helpmebackend.activity.domain.entity.Activity;
import seungyong.helpmebackend.activity.domain.entity.ActivityPage;
import seungyong.helpmebackend.activity.domain.type.ActivityType;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.filter.AuthenticationFilter;
import seungyong.helpmebackend.global.infrastructure.cookie.CookieUtil;
import seungyong.helpmebackend.support.config.TestSecurityConfig;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = ActivityController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AuthenticationFilter.class
        )
)
@Import(TestSecurityConfig.class)
class ActivityControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private ActivityPortIn activityPortIn;
    @MockitoBean private CookieUtil cookieUtil;

    @Test
    void returnsTimelineSummaryAndOpaquePage() throws Exception {
        Activity item = Activity.builder()
                .id(801L).projectId(101L).externalKey("commit:main:abc")
                .type(ActivityType.PUSH_COMMIT).branchName("main").commitSha("abc")
                .title("feat: activity").summary("Webhook flow")
                .actorLogin("octocat").filesChanged(4)
                .occurredAt(OffsetDateTime.parse("2026-08-17T00:00:00Z"))
                .details(Map.of()).build();
        given(activityPortIn.getActivities(
                eq(1L), eq(101L), eq("webhook"), eq("main"), eq("push_commit"),
                any(), any(), eq(null), eq(20)
        )).willReturn(new ActivityPage(
                List.of(item), new ActivityPage.Summary(1, 1, 4, 1),
                "next", true, true
        ));

        mockMvc.perform(get("/api/v1/projects/101/activities")
                        .param("q", "webhook")
                        .param("branch", "main")
                        .param("type", "push_commit")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-17")
                        .param("size", "20")
                        .with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(801L))
                .andExpect(jsonPath("$.items[0].type").value("push_commit"))
                .andExpect(jsonPath("$.items[0].publicUrl").value((Object) null))
                .andExpect(jsonPath("$.summary.commitCount").value(1))
                .andExpect(jsonPath("$.page.nextCursor").value("next"))
                .andExpect(jsonPath("$.filtersApplied").value(true));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor user() {
        return SecurityMockMvcRequestPostProcessors.user(
                new CustomUserDetails(1L, "test-user")
        );
    }
}
