package seungyong.helpmebackend.auth.adapter.out;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import seungyong.helpmebackend.auth.adapter.out.github.OAuth2Adapter;
import seungyong.helpmebackend.auth.application.port.out.result.OAuthGithubUser;
import seungyong.helpmebackend.auth.application.port.out.result.OAuthTokenResult;
import seungyong.helpmebackend.auth.domain.entity.Installation;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.global.infrastructure.github.GithubApiExecutor;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OAuth2AdapterTest {

    @Mock private GithubApiExecutor githubApiExecutor;

    @InjectMocks private OAuth2Adapter oAuth2Adapter;

    @Captor private ArgumentCaptor<GithubApiExecutor.JsonResponseParser<OAuthGithubUser>> userParserCaptor;
    @Captor private ArgumentCaptor<GithubApiExecutor.JsonResponseParser<List<Installation>>> installationParserCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // DTO나 Record 객체 생성을 위한 FixtureMonkey 기본 설정 (생성자 기반)
    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
            .defaultNotNull(true)
            .build();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(oAuth2Adapter, "clientId", "test-client-id");
        ReflectionTestUtils.setField(oAuth2Adapter, "clientSecret", "test-client-secret");
        ReflectionTestUtils.setField(oAuth2Adapter, "redirectUri", "test-redirect-uri");
    }

    @Nested
    @DisplayName("generateLoginUrl - 로그인 URL 생성")
    class GenerateLoginUrl {
        @Test
        @DisplayName("성공")
        void generateLoginUrl_success() {
            String state = fixtureMonkey.giveMeOne(String.class);

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
            String code = fixtureMonkey.giveMeOne(String.class);
            OAuthTokenResult expectedResult = fixtureMonkey.giveMeOne(OAuthTokenResult.class);

            given(githubApiExecutor.executePostNoAuth(anyString(), anyMap(), eq(OAuthTokenResult.class), anyString()))
                    .willReturn(expectedResult);

            OAuthTokenResult result = oAuth2Adapter.getAccessToken(code);

            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        @DisplayName("실패 (API 통신 오류)")
        void getAccessToken_failure_apiError() {
            String code = fixtureMonkey.giveMeOne(String.class);

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
            String accessToken = fixtureMonkey.giveMeOne(String.class);
            OAuthGithubUser expectedUser = fixtureMonkey.giveMeOne(OAuthGithubUser.class);

            given(githubApiExecutor.executeGet(anyString(), eq(accessToken), any(), anyString()))
                    .willReturn(expectedUser);

            OAuthGithubUser result = oAuth2Adapter.getGithubUser(accessToken);

            assertThat(result).isEqualTo(expectedUser);
        }

        @Test
        @DisplayName("성공 (JSON 파싱 로직 검증)")
        void getGithubUser_success_parser() throws Exception {
            String accessToken = fixtureMonkey.giveMeOne(String.class);
            OAuthGithubUser expectedUser = fixtureMonkey.giveMeOne(OAuthGithubUser.class);

            String jsonString = String.format("{\"login\":\"%s\", \"id\":%d}", expectedUser.name(), expectedUser.githubId());
            JsonNode mockJsonNode = objectMapper.readTree(jsonString);

            given(githubApiExecutor.executeGet(anyString(), eq(accessToken), userParserCaptor.capture(), anyString()))
                    .willReturn(fixtureMonkey.giveMeOne(OAuthGithubUser.class));

            oAuth2Adapter.getGithubUser(accessToken);

            OAuthGithubUser parsedUser = userParserCaptor.getValue().parse(mockJsonNode);

            assertThat(parsedUser.githubId()).isEqualTo(expectedUser.githubId());
            assertThat(parsedUser.name()).isEqualTo(expectedUser.name());
        }

        @Test
        @DisplayName("실패 (API 통신 오류)")
        void getGithubUser_failure_apiError() {
            String accessToken = fixtureMonkey.giveMeOne(String.class);

            given(githubApiExecutor.executeGet(anyString(), eq(accessToken), any(), anyString()))
                    .willThrow(new CustomException(GlobalErrorCode.GITHUB_ERROR));

            assertThatThrownBy(() -> oAuth2Adapter.getGithubUser(accessToken))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("getInstallations - 깃허브 설치 정보 조회")
    class GetInstallations {
        @Test
        @DisplayName("성공")
        void getInstallations_success() {
            String accessToken = fixtureMonkey.giveMeOne(String.class);
            List<Installation> expectedInstallations = fixtureMonkey.giveMeBuilder(Installation.class)
                    .sampleList(3);

            given(githubApiExecutor.executeGet(anyString(), eq(accessToken), any(), anyString()))
                    .willReturn(expectedInstallations);

            List<Installation> result = oAuth2Adapter.getInstallations(accessToken);

            assertThat(result).isEqualTo(expectedInstallations);
        }

        @Test
        @DisplayName("성공 (JSON 파싱 로직 검증)")
        void getInstallations_success_parser() throws Exception {
            String accessToken = fixtureMonkey.giveMeOne(String.class);
            Installation expectedInstallation = fixtureMonkey.giveMeOne(Installation.class);

            String jsonString = String.format("""
                    {
                        "installations": [
                            {
                                "id": "%s",
                                "account": {
                                    "avatar_url": "%s",
                                    "login": "%s"
                                }
                            }
                        ]
                    }
                    """, expectedInstallation.getInstallationId(), expectedInstallation.getAvatarUrl(), expectedInstallation.getName());
            JsonNode mockJsonNode = objectMapper.readTree(jsonString);

            // 실제 API 호출을 막기위해 모킹
            given(githubApiExecutor.executeGet(anyString(), eq(accessToken), installationParserCaptor.capture(), anyString()))
                    .willReturn(List.of());

            oAuth2Adapter.getInstallations(accessToken);

            // 캡처된 인자값인 json 익명 함수를 가져와 파싱 후 결과 검증 (GithubApiExecutor.JsonResponseParser<List<Installation>>)
            List<Installation> parsedInstallations = installationParserCaptor.getValue().parse(mockJsonNode);

            assertThat(parsedInstallations).hasSize(1);
            assertThat(parsedInstallations.get(0).getInstallationId()).isEqualTo(expectedInstallation.getInstallationId());
            assertThat(parsedInstallations.get(0).getAvatarUrl()).isEqualTo(expectedInstallation.getAvatarUrl());
            assertThat(parsedInstallations.get(0).getName()).isEqualTo(expectedInstallation.getName());
        }

        @Test
        @DisplayName("실패 (API 통신 오류)")
        void getInstallations_failure_apiError() {
            String accessToken = fixtureMonkey.giveMeOne(String.class);

            given(githubApiExecutor.executeGet(anyString(), eq(accessToken), any(), anyString()))
                    .willThrow(new CustomException(GlobalErrorCode.GITHUB_ERROR));

            assertThatThrownBy(() -> oAuth2Adapter.getInstallations(accessToken))
                    .isInstanceOf(CustomException.class);
        }
    }
}