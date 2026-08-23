package seungyong.helpmebackend.devlog;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.global.config.SecurityConfig;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.infrastructure.jwt.JWTProvider;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.entity.ProjectSettings;
import seungyong.helpmebackend.project.domain.type.ReflectionWeekday;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.GithubUser;
import seungyong.helpmebackend.user.domain.entity.JWTUser;
import seungyong.helpmebackend.user.domain.entity.User;

import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(SecurityConfig.class)
class DevlogIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JWTProvider jwtProvider;
    @Autowired private CipherPortOut cipherPortOut;
    @Autowired private UserPortOut userPortOut;
    @Autowired private ProjectPortOut projectPortOut;

    @Test
    @DisplayName("프로젝트 timezone 날짜의 빈 조회부터 생성·수정·충돌·삭제까지 HTTP와 DB를 통합")
    void devlogLifecycle_success() throws Exception {
        User user = saveUser();
        Project project = saveProject(user.getId());
        Cookie[] cookies = cookies(user);
        String path = "/api/v1/projects/{projectId}/devlogs/{logDate}";

        mockMvc.perform(get(path, project.getId(), "2026-08-22").cookie(cookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false))
                .andExpect(jsonPath("$.projectId").value(project.getId()))
                .andExpect(jsonPath("$.logDate").value("2026-08-22"))
                .andExpect(jsonPath("$.version").value(nullValue()));

        mockMvc.perform(put(path, project.getId(), "2026-08-22").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentMd":"첫 개발로그","version":null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.contentMd").value("첫 개발로그"))
                .andExpect(jsonPath("$.version").value(0));

        mockMvc.perform(put(path, project.getId(), "2026-08-22").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentMd":"수정된 개발로그","version":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentMd").value("수정된 개발로그"))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(put(path, project.getId(), "2026-08-22").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentMd":"오래된 수정","version":0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DOCUMENT_40901"));

        mockMvc.perform(put(path, project.getId(), "2026-08-22").cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentMd":"","version":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false))
                .andExpect(jsonPath("$.id").value(nullValue()))
                .andExpect(jsonPath("$.version").value(nullValue()));

        mockMvc.perform(get(path, project.getId(), "2026-08-22").cookie(cookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }

    private User saveUser() {
        return userPortOut.save(new User(
                null,
                new GithubUser(
                        "devlog-user",
                        818181L,
                        new EncryptedToken(cipherPortOut.encrypt("raw-github-token"))
                )
        ));
    }

    private Project saveProject(Long userId) {
        return projectPortOut.save(Project.builder()
                .userId(userId)
                .repoFullName("devlog-user/timezone-project")
                .githubRepoId(919191L)
                .githubInstallationId(9001L)
                .defaultBranch("main")
                .settings(new ProjectSettings(
                        List.of("main"),
                        false,
                        "America/New_York",
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
