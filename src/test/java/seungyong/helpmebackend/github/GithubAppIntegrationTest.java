package seungyong.helpmebackend.github;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.github.application.port.out.GithubAppPortOut;
import seungyong.helpmebackend.github.domain.entity.GithubInstallation;
import seungyong.helpmebackend.github.domain.entity.GithubRepository;
import seungyong.helpmebackend.github.domain.entity.GithubRepositoryPage;
import seungyong.helpmebackend.github.domain.exception.GithubErrorCode;
import seungyong.helpmebackend.github.domain.type.GithubAccountType;
import seungyong.helpmebackend.github.domain.type.GithubRepositorySelection;
import seungyong.helpmebackend.global.config.SecurityConfig;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.infrastructure.jwt.JWTProvider;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.GithubUser;
import seungyong.helpmebackend.user.domain.entity.JWTUser;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.user.domain.type.GithubTokenStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(SecurityConfig.class)
class GithubAppIntegrationTest {
    private static final String RAW_TOKEN = "raw-github-token";
    private static final Long INSTALLATION_ID = 9001L;

    @Autowired private MockMvc mockMvc;
    @Autowired private JWTProvider jwtProvider;
    @Autowired private CipherPortOut cipherPortOut;
    @Autowired private UserPortOut userPortOut;
    @MockitoBean private GithubAppPortOut githubAppPortOut;

    @Test
    @DisplayName("설치 계정 조회 - 인증, 복호화, GitHub 결과, token 검증 저장을 통합")
    void getInstallations_success() throws Exception {
        User savedUser = saveUser();
        given(githubAppPortOut.getInstallations(savedUser.getId(), RAW_TOKEN))
                .willReturn(List.of(new GithubInstallation(
                        INSTALLATION_ID,
                        "seungyong",
                        GithubAccountType.USER,
                        GithubRepositorySelection.SELECTED,
                        8
                )));

        mockMvc.perform(get("/api/v1/github/installations").cookie(cookies(savedUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appInstalled").value(true))
                .andExpect(jsonPath("$.accounts[0].installationId").value(INSTALLATION_ID))
                .andExpect(jsonPath("$.accounts[0].repositoryCount").value(8));

        User verifiedUser = userPortOut.getById(savedUser.getId());
        assertThat(verifiedUser.getGithubUser().getTokenStatus()).isEqualTo(GithubTokenStatus.VALID);
        assertThat(verifiedUser.getGithubUser().getTokenVerifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("설치 Repository 조회 - canonical 정보와 DB 중복 상태를 통합")
    void getRepositories_success() throws Exception {
        User savedUser = saveUser();
        GithubRepository repository = new GithubRepository(
                778899L,
                "seungyong/helpme.md",
                true,
                "main",
                new GithubRepository.Permissions(true, true)
        );
        given(githubAppPortOut.getRepositories(
                savedUser.getId(), RAW_TOKEN, INSTALLATION_ID, "", 1, 30
        )).willReturn(new GithubRepositoryPage(List.of(repository), null, false));

        mockMvc.perform(get(
                        "/api/v1/github/installations/{installationId}/repositories",
                        INSTALLATION_ID
                ).cookie(cookies(savedUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].githubRepoId").value(778899L))
                .andExpect(jsonPath("$.items[0].fullName").value("seungyong/helpme.md"))
                .andExpect(jsonPath("$.items[0].isPrivate").value(true))
                .andExpect(jsonPath("$.items[0].branches").doesNotExist())
                .andExpect(jsonPath("$.items[0].alreadyConnected").value(false))
                .andExpect(jsonPath("$.page.hasNext").value(false));
    }

    @Test
    @DisplayName("GitHub 연결 회수 - 403 반환 후 users token status를 revoked로 저장")
    void getInstallations_failure_connectionRevoked() throws Exception {
        User savedUser = saveUser();
        given(githubAppPortOut.getInstallations(savedUser.getId(), RAW_TOKEN))
                .willThrow(new CustomException(GithubErrorCode.GITHUB_CONNECTION_REVOKED));

        mockMvc.perform(get("/api/v1/github/installations").cookie(cookies(savedUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("GITHUB_40301"));

        assertThat(userPortOut.getById(savedUser.getId()).getGithubUser().getTokenStatus())
                .isEqualTo(GithubTokenStatus.REVOKED);
    }

    @Test
    @DisplayName("인증 없음은 AUTH_40101")
    void getInstallations_failure_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/github/installations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_40101"));
    }

    @Test
    @DisplayName("잘못된 Access Token은 AUTH_40102와 쿠키 제거")
    void getInstallations_failure_invalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/github/installations")
                        .cookie(
                                new Cookie("accessToken", "invalid-token"),
                                new Cookie("refreshToken", "refresh-token")
                        ))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_40102"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .cookie().maxAge("accessToken", 0))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .cookie().maxAge("refreshToken", 0));
    }

    private User saveUser() {
        String encryptedToken = cipherPortOut.encrypt(RAW_TOKEN);
        return userPortOut.save(new User(
                null,
                new GithubUser("seungyong", 1001L, new EncryptedToken(encryptedToken))
        ));
    }

    private Cookie[] cookies(User user) {
        JWT jwt = jwtProvider.generate(new JWTUser(user.getId(), user.getGithubUser().getName()));
        return new Cookie[] {
                new Cookie("accessToken", jwt.getAccessToken()),
                new Cookie("refreshToken", jwt.getRefreshToken())
        };
    }
}
