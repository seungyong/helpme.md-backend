package seungyong.helpmebackend.repository.adapter.out.gpt;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.repository.application.port.out.command.EvaluationCommand;
import seungyong.helpmebackend.repository.application.port.out.command.GenerateReadmeCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepositoryInfoCommand;
import seungyong.helpmebackend.repository.application.port.out.result.EvaluationContentResult;
import seungyong.helpmebackend.repository.application.port.out.result.GPTRepositoryInfoResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GPTAdapterTest {
    @Mock private GPTClient gptClient;

    @InjectMocks private GPTAdapter gptAdapter;

    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
            .defaultNotNull(true)
            .build();

    @Nested
    @DisplayName("getRepositoryInfo - 리포지토리 정보 분석")
    class GetRepositoryInfo {
        @Test
        @DisplayName("성공")
        void getRepositoryInfo_success() {
            String fullName = "owner/repo";
            RepositoryInfoCommand command = fixtureMonkey.giveMeOne(RepositoryInfoCommand.class);
            GPTRepositoryInfoResult expectedResult = fixtureMonkey.giveMeOne(GPTRepositoryInfoResult.class);

            given(gptClient.getRepositoryInfo(fullName, command)).willReturn(expectedResult);

            GPTRepositoryInfoResult actualResult = gptAdapter.getRepositoryInfo(fullName, command);

            assertThat(actualResult).isEqualTo(expectedResult);
        }

        @Test
        @DisplayName("실패 - GPT 오류")
        void getRepositoryInfo_failure() {
            String fullName = "owner/repo";
            RepositoryInfoCommand command = fixtureMonkey.giveMeOne(RepositoryInfoCommand.class);

            given(gptClient.getRepositoryInfo(anyString(), any(RepositoryInfoCommand.class)))
                    .willThrow(new RuntimeException("API 연동 오류"));

            assertThatThrownBy(() -> gptAdapter.getRepositoryInfo(fullName, command))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.GPT_ERROR);
        }
    }

    @Nested
    @DisplayName("evaluateReadme - README 평가")
    class EvaluateReadme {
        @Test
        @DisplayName("성공")
        void evaluateReadme_success() {
            EvaluationCommand command = fixtureMonkey.giveMeOne(EvaluationCommand.class);
            EvaluationContentResult expectedResult = fixtureMonkey.giveMeOne(EvaluationContentResult.class);

            given(gptClient.evaluateReadme(command)).willReturn(expectedResult);

            EvaluationContentResult actualResult = gptAdapter.evaluateReadme(command);

            assertThat(actualResult).isEqualTo(expectedResult);
        }

        @Test
        @DisplayName("실패 - GPT 오류")
        void evaluateReadme_failure() {
            EvaluationCommand command = fixtureMonkey.giveMeOne(EvaluationCommand.class);

            given(gptClient.evaluateReadme(any(EvaluationCommand.class)))
                    .willThrow(new RuntimeException("API Timeout"));

            assertThatThrownBy(() -> gptAdapter.evaluateReadme(command))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.GPT_ERROR);
        }
    }

    @Nested
    @DisplayName("generateDraftReadme - README 초안 생성")
    class GenerateDraftReadme {
        @Test
        @DisplayName("성공")
        void generateDraftReadme_success() {
            GenerateReadmeCommand command = fixtureMonkey.giveMeOne(GenerateReadmeCommand.class);
            String expectedResult = "# Generated README";

            given(gptClient.generateDraftReadme(command)).willReturn(expectedResult);

            String actualResult = gptAdapter.generateDraftReadme(command);

            assertThat(actualResult).isEqualTo(expectedResult);
        }

        @Test
        @DisplayName("실패 - GPTClient에서 예외 발생 시 CustomException으로 변환")
        void generateDraftReadme_failure() {
            GenerateReadmeCommand command = fixtureMonkey.giveMeOne(GenerateReadmeCommand.class);

            given(gptClient.generateDraftReadme(any(GenerateReadmeCommand.class)))
                    .willThrow(new RuntimeException("GPT 토큰 부족"));

            assertThatThrownBy(() -> gptAdapter.generateDraftReadme(command))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.GPT_ERROR);
        }
    }
}