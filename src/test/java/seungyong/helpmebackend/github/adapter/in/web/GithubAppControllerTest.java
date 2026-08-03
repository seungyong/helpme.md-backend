package seungyong.helpmebackend.github.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import seungyong.helpmebackend.github.application.port.in.GithubAppPortIn;
import seungyong.helpmebackend.github.application.port.in.result.GithubInstallationsResult;
import seungyong.helpmebackend.github.application.port.in.result.GithubRepositoriesResult;
import seungyong.helpmebackend.github.domain.entity.GithubInstallation;
import seungyong.helpmebackend.github.domain.entity.GithubRepository;
import seungyong.helpmebackend.github.domain.type.GithubAccountType;
import seungyong.helpmebackend.github.domain.type.GithubRepositorySelection;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.filter.AuthenticationFilter;
import seungyong.helpmebackend.global.infrastructure.cookie.CookieUtil;
import seungyong.helpmebackend.support.config.TestSecurityConfig;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = GithubAppController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AuthenticationFilter.class
        )
)
@Import(TestSecurityConfig.class)
class GithubAppControllerTest {
    private static final Long USER_ID = 1L;
    private static final Long INSTALLATION_ID = 9001L;

    @Autowired private MockMvc mockMvc;
    @MockitoBean private GithubAppPortIn githubAppPortIn;
    @MockitoBean private CookieUtil cookieUtil;

    @Test
    @DisplayName("GitHub 설치 계정 응답이 API 계약을 따른다")
    void getInstallations_success() throws Exception {
        given(githubAppPortIn.getInstallations(USER_ID)).willReturn(
                new GithubInstallationsResult(
                        true,
                        List.of(new GithubInstallation(
                                INSTALLATION_ID,
                                "seungyong",
                                GithubAccountType.USER,
                                GithubRepositorySelection.SELECTED,
                                8
                        ))
                )
        );

        mockMvc.perform(get("/api/v1/github/installations").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appInstalled").value(true))
                .andExpect(jsonPath("$.accounts[0].installationId").value(INSTALLATION_ID))
                .andExpect(jsonPath("$.accounts[0].login").value("seungyong"))
                .andExpect(jsonPath("$.accounts[0].type").value("User"))
                .andExpect(jsonPath("$.accounts[0].repositorySelection").value("selected"))
                .andExpect(jsonPath("$.accounts[0].repositoryCount").value(8));
    }

    @Test
    @DisplayName("GitHub 설치 Repository 응답과 query parameter 전달이 API 계약을 따른다")
    void getRepositories_success() throws Exception {
        GithubRepository repository = new GithubRepository(
                778899L,
                "seungyong/helpme.md",
                true,
                "main",
                new GithubRepository.Permissions(true, true)
        );
        given(githubAppPortIn.getRepositories(
                USER_ID, INSTALLATION_ID, "help", "2", 10
        )).willReturn(new GithubRepositoriesResult(
                List.of(new GithubRepositoriesResult.Item(repository, false)),
                new GithubRepositoriesResult.Page(null, false)
        ));

        mockMvc.perform(get("/api/v1/github/installations/{installationId}/repositories", INSTALLATION_ID)
                        .param("q", "help")
                        .param("cursor", "2")
                        .param("size", "10")
                        .with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].githubRepoId").value(778899L))
                .andExpect(jsonPath("$.items[0].fullName").value("seungyong/helpme.md"))
                .andExpect(jsonPath("$.items[0].isPrivate").value(true))
                .andExpect(jsonPath("$.items[0].branches").doesNotExist())
                .andExpect(jsonPath("$.items[0].permissions.admin").value(true))
                .andExpect(jsonPath("$.items[0].alreadyConnected").value(false))
                .andExpect(jsonPath("$.page.hasNext").value(false));

        verify(githubAppPortIn).getRepositories(
                USER_ID, INSTALLATION_ID, "help", "2", 10
        );
    }

    private RequestPostProcessor user() {
        return SecurityMockMvcRequestPostProcessors.user(
                new CustomUserDetails(USER_ID, "seungyong")
        );
    }
}
