package seungyong.helpmebackend.project;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.github.application.port.out.GithubAppPortOut;
import seungyong.helpmebackend.global.config.SecurityConfig;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.infrastructure.jwt.JWTProvider;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.entity.ProjectSettings;
import seungyong.helpmebackend.project.domain.type.ProjectStatus;
import seungyong.helpmebackend.project.domain.type.ReflectionWeekday;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.GithubUser;
import seungyong.helpmebackend.user.domain.entity.JWTUser;
import seungyong.helpmebackend.user.domain.entity.User;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(SecurityConfig.class)
class ProjectIntegrationTest {
    private static final String RAW_TOKEN = "raw-github-token";

    @Autowired private MockMvc mockMvc;
    @Autowired private JWTProvider jwtProvider;
    @Autowired private CipherPortOut cipherPortOut;
    @Autowired private UserPortOut userPortOut;
    @Autowired private ProjectPortOut projectPortOut;
    @MockitoBean private GithubAppPortOut githubAppPortOut;

    @Test
    @DisplayName("프로젝트 상세 조회와 Branch 검증 설정 수정을 HTTP부터 DB까지 통합")
    void getAndUpdateProjectSettings_success() throws Exception {
        User user = saveUser("seungyong", 1001L);
        Project project = saveProject(user.getId(), "seungyong/helpme.md", ProjectStatus.ACTIVE);
        mockMvc.perform(get("/api/v1/projects/{projectId}", project.getId())
                        .cookie(cookies(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId()))
                .andExpect(jsonPath("$.repoFullname").value("seungyong/helpme.md"))
                .andExpect(jsonPath("$.sync.status").value("pending"))
                .andExpect(jsonPath("$.webhook.status").value("waiting"));

        mockMvc.perform(patch("/api/v1/projects/{projectId}/settings", project.getId())
                        .cookie(cookies(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "trackedBranches":["main","develop"],
                                  "trackAllBranches":false,
                                  "timezone":"America/New_York",
                                  "dailyEnabled":false,
                                  "weeklyGenerationDay":"friday",
                                  "webhookPayloadRetentionDays":14
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackedBranches[1]").value("develop"))
                .andExpect(jsonPath("$.timezone").value("America/New_York"))
                .andExpect(jsonPath("$.daily.enabled").value(false))
                .andExpect(jsonPath("$.daily.generationTime").value("23:30"))
                .andExpect(jsonPath("$.weekly.generationDay").value("friday"))
                .andExpect(jsonPath("$.webhookPayloadRetentionDays").value(14))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        Project persisted = projectPortOut.getById(project.getId()).orElseThrow();
        assertThat(persisted.getSettings().trackedBranches()).containsExactly("main", "develop");
        assertThat(persisted.getSettings().timezone()).isEqualTo("America/New_York");
        assertThat(persisted.getSync().status().getDatabaseValue()).isEqualTo("pending");
        assertThat(persisted.getWebhook().status().getDatabaseValue()).isEqualTo("waiting");
    }

    @Test
    @DisplayName("없음·타인 소유·비활성 프로젝트를 404·403·409로 구분")
    void getProject_failure_accessContract() throws Exception {
        User owner = saveUser("owner", 1001L);
        User other = saveUser("other", 1002L);
        Project active = saveProject(owner.getId(), "owner/active", ProjectStatus.ACTIVE);
        Project inactive = saveProject(owner.getId(), "owner/deleting", ProjectStatus.DELETING);

        mockMvc.perform(get("/api/v1/projects/{projectId}", 999999L)
                        .cookie(cookies(owner)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PROJECT_40401"));

        mockMvc.perform(get("/api/v1/projects/{projectId}", active.getId())
                        .cookie(cookies(other)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PROJECT_40301"));

        mockMvc.perform(get("/api/v1/projects/{projectId}/settings", inactive.getId())
                        .cookie(cookies(owner)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PROJECT_40903"));
    }

    private User saveUser(String name, long githubId) {
        return userPortOut.save(new User(
                null,
                new GithubUser(
                        name,
                        githubId,
                        new EncryptedToken(cipherPortOut.encrypt(RAW_TOKEN + githubId))
                )
        ));
    }

    private Project saveProject(Long userId, String repoFullName, ProjectStatus status) {
        return projectPortOut.save(Project.builder()
                .userId(userId)
                .repoFullName(repoFullName)
                .githubRepoId(Math.abs((long) repoFullName.hashCode()))
                .githubInstallationId(9001L)
                .defaultBranch("main")
                .status(status)
                .settings(new ProjectSettings(
                        List.of("main"),
                        false,
                        "Asia/Seoul",
                        new ProjectSettings.DailyReflectionSchedule(true, LocalTime.of(23, 30)),
                        new ProjectSettings.WeeklyReflectionSchedule(
                                true, ReflectionWeekday.SUNDAY, LocalTime.of(23, 50)
                        ),
                        (short) 30
                ))
                .build());
    }

    private Cookie[] cookies(User user) {
        JWT jwt = jwtProvider.generate(new JWTUser(user.getId(), user.getGithubUser().getName()));
        return new Cookie[] {
                new Cookie("accessToken", jwt.getAccessToken()),
                new Cookie("refreshToken", jwt.getRefreshToken())
        };
    }
}
