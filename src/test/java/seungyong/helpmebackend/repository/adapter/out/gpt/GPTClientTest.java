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
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import seungyong.helpmebackend.repository.application.port.out.command.EvaluationCommand;
import seungyong.helpmebackend.repository.application.port.out.command.GenerateReadmeCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepositoryInfoCommand;
import seungyong.helpmebackend.repository.application.port.out.result.EvaluationContentResult;
import seungyong.helpmebackend.repository.application.port.out.result.GPTRepositoryInfoResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class GPTClientTest {
    @Mock private ChatModel chatModel;

    @InjectMocks private GPTClient gptClient;

    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
            .defaultNotNull(true)
            .build();

    private void mockChatResponse(String jsonResponse) {
        ChatResponse response = mock(ChatResponse.class, RETURNS_DEEP_STUBS);

        given(response.getResult().getOutput().getText()).willReturn(jsonResponse);
        given(response.getMetadata().getUsage().getPromptTokens()).willReturn(100);
        given(response.getMetadata().getUsage().getCompletionTokens()).willReturn(50);
        given(response.getMetadata().getUsage().getTotalTokens()).willReturn(150);

        given(chatModel.call(any(Prompt.class))).willReturn(response);
    }

    @Nested
    @DisplayName("getRepositoryInfo - 리포지토리 정보 분석")
    class GetRepositoryInfo {
        @Test
        @DisplayName("성공")
        void getRepositoryInfo_success() {
            String fullName = "owner/repo";
            RepositoryInfoCommand command = fixtureMonkey.giveMeOne(RepositoryInfoCommand.class);

            String jsonResponse = "{\"techStack\": [\"Java\", \"Spring\"], \"projectSize\": \"medium\", \"entryPoints\": [\"src/main/Main.java\"], \"importantFiles\": [\"pom.xml\"]}";
            mockChatResponse(jsonResponse);

            GPTRepositoryInfoResult result = gptClient.getRepositoryInfo(fullName, command);

            assertThat(result.techStack()).containsExactly("Java", "Spring");
            assertThat(result.projectSize()).isEqualTo("medium");
            assertThat(result.entryPoints()).containsExactly("src/main/Main.java");
            assertThat(result.importantFiles()).containsExactly("pom.xml");
        }
    }

    @Nested
    @DisplayName("evaluateReadme - README 평가")
    class EvaluateReadme {
        @Test
        @DisplayName("성공")
        void evaluateReadme_success() {
            EvaluationCommand command = fixtureMonkey.giveMeOne(EvaluationCommand.class);

            String jsonResponse = "{\"rating\": 85, \"contents\": [\"장점: 설명이 명확합니다.\", \"개선: 예제 코드가 추가되면 좋겠습니다.\"]}";
            mockChatResponse(jsonResponse);

            EvaluationContentResult result = gptClient.evaluateReadme(command);

            assertThat(result.rating()).isEqualTo(85);
            assertThat(result.contents()).containsExactly("장점: 설명이 명확합니다.", "개선: 예제 코드가 추가되면 좋겠습니다.");
        }
    }

    @Nested
    @DisplayName("generateDraftReadme - README 초안 생성")
    class GenerateDraftReadme {
        @Test
        @DisplayName("성공")
        void generateDraftReadme_success() {
            GenerateReadmeCommand command = fixtureMonkey.giveMeOne(GenerateReadmeCommand.class);

            String jsonResponse = "{\"content\": \"# Generated README\\nThis is a test.\"}";
            mockChatResponse(jsonResponse);

            String result = gptClient.generateDraftReadme(command);

            assertThat(result).isEqualTo("# Generated README\nThis is a test.");
        }
    }
}