package seungyong.helpmebackend.auth.adapter.out;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.auth.adapter.out.github.OAuth2Adapter;
import seungyong.helpmebackend.auth.application.port.out.result.OAuthGithubUser;
import seungyong.helpmebackend.auth.application.port.out.result.OAuthTokenResult;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.global.config.GithubPortConfig;
import seungyong.helpmebackend.global.infrastructure.github.GithubApiExecutor;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static seungyong.helpmebackend.support.fixture.TestFixtures.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AdapterTest {

    @Mock private GithubApiExecutor githubApiExecutor;
    private OAuth2Adapter oAuth2Adapter;

    @Captor private ArgumentCaptor<GithubApiExecutor.JsonResponseParser<OAuthGithubUser>> userParserCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        oAuth2Adapter = new OAuth2Adapter(
                githubApiExecutor,
                new GithubPortConfig("test-client-id", "test-client-secret", "test-redirect-uri")
        );
    }

    @Nested
    @DisplayName("generateLoginUrl - 로그인 URL 생성")
    class GenerateLoginUrl {
        @Test
        @DisplayName("성공")
        void generateLoginUrl_success() {
            String state = "oauth-state";

            String result = oAuth2Adapter.generateLoginUrl(state);

            assertThat(result).contains("client_id=test-client-id");
            assertThat(result).contains("redirect_uri=test-redirect-uri");
            assertThat(result).contains("state=" + state);
        }
    }

    @Nested
    @DisplayName("getAccessToken - 액세스 토큰 발급")
    class GetAccessToken {
        @Test
        @DisplayName("성공")
        void getAccessToken_success() {
            String code = "authorization-code";
            OAuthTokenResult expectedResult = oauthTokenResult();

            given(githubApiExecutor.executePostNoAuth(anyString(), anyMap(), eq(OAuthTokenResult.class), anyString()))
                    .willReturn(expectedResult);

            OAuthTokenResult result = oAuth2Adapter.getAccessToken(code);

            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        @DisplayName("실패 (API 통신 오류)")
        void getAccessToken_failure_apiError() {
            String code = "authorization-code";

            given(githubApiExecutor.executePostNoAuth(anyString(), anyMap(), eq(OAuthTokenResult.class), anyString()))
                    .willThrow(new CustomException(GlobalErrorCode.GITHUB_ERROR));

            assertThatThrownBy(() -> oAuth2Adapter.getAccessToken(code))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("getGithubUser - 깃허브 유저 정보 조회")
    class GetGithubUser {
        @Test
        @DisplayName("성공")
        void getGithubUser_success() {
            String accessToken = "github-access-token";
            OAuthGithubUser expectedUser = oauthGithubUser();

            given(githubApiExecutor.executeGet(anyString(), eq(accessToken), any(), anyString()))
                    .willReturn(expectedUser);

            OAuthGithubUser result = oAuth2Adapter.getGithubUser(accessToken);

            assertThat(result).isEqualTo(expectedUser);
        }

        @Test
        @DisplayName("성공 (JSON 파싱 로직 검증)")
        void getGithubUser_success_parser() throws Exception {
            String accessToken = "github-access-token";
            OAuthGithubUser expectedUser = oauthGithubUser();

            String jsonString = String.format("{\"login\":\"%s\", \"id\":%d}", expectedUser.name(), expectedUser.githubId());
            JsonNode mockJsonNode = objectMapper.readTree(jsonString);

            given(githubApiExecutor.executeGet(anyString(), eq(accessToken), userParserCaptor.capture(), anyString()))
                    .willReturn(oauthGithubUser());

            oAuth2Adapter.getGithubUser(accessToken);

            OAuthGithubUser parsedUser = userParserCaptor.getValue().parse(mockJsonNode);

            assertThat(parsedUser.githubId()).isEqualTo(expectedUser.githubId());
            assertThat(parsedUser.name()).isEqualTo(expectedUser.name());
        }

        @Test
        @DisplayName("실패 (API 통신 오류)")
        void getGithubUser_failure_apiError() {
            String accessToken = "github-access-token";

            given(githubApiExecutor.executeGet(anyString(), eq(accessToken), any(), anyString()))
                    .willThrow(new CustomException(GlobalErrorCode.GITHUB_ERROR));

            assertThatThrownBy(() -> oAuth2Adapter.getGithubUser(accessToken))
                    .isInstanceOf(CustomException.class);
        }
    }
}
