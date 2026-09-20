package seungyong.helpmebackend.project.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
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
import seungyong.helpmebackend.project.application.port.in.ProjectDeletionPortIn;
import seungyong.helpmebackend.project.application.port.in.command.DeleteProjectCommand;
import seungyong.helpmebackend.project.application.port.in.result.ProjectDeletionResult;
import seungyong.helpmebackend.support.config.TestSecurityConfig;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = ProjectDeletionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AuthenticationFilter.class
        )
)
@Import(TestSecurityConfig.class)
class ProjectDeletionControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private ProjectDeletionPortIn projectDeletionPortIn;
    @MockitoBean private CookieUtil cookieUtil;

    @Test
    @DisplayName("프로젝트 삭제 작업을 저장한 202 상태를 반환")
    void acceptsDeletion() throws Exception {
        OffsetDateTime requestedAt = OffsetDateTime.parse("2026-09-14T10:00:00Z");
        when(projectDeletionPortIn.requestDeletion(any(DeleteProjectCommand.class)))
                .thenReturn(new ProjectDeletionResult(101L, "deleting", requestedAt));

        mockMvc.perform(delete("/api/v1/projects/101")
                        .with(SecurityMockMvcRequestPostProcessors.user(
                                new CustomUserDetails(1L, "seungyong")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmationRepoFullname\":\"seungyong/helpme.md\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.projectId").value(101L))
                .andExpect(jsonPath("$.status").value("deleting"))
                .andExpect(jsonPath("$.requestedAt").value("2026-09-14T10:00:00Z"));
    }

    @Test
    @DisplayName("Repository 확인 문자열이 비어 있으면 400")
    void rejectsBlankConfirmation() throws Exception {
        mockMvc.perform(delete("/api/v1/projects/101")
                        .with(SecurityMockMvcRequestPostProcessors.user(
                                new CustomUserDetails(1L, "seungyong")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmationRepoFullname\":\" \"}"))
                .andExpect(status().isBadRequest());

        verify(projectDeletionPortIn, never()).requestDeletion(any());
    }
}
