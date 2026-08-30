package seungyong.helpmebackend.reflection.adapter.out.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionSourceSnapshot;
import seungyong.helpmebackend.reflection.domain.exception.ReflectionGenerationException;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ReflectionAiAdapterTest {
    @Mock private ChatModel chatModel;
    private ReflectionAiAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ReflectionAiAdapter(chatModel);
    }

    @Test
    @DisplayName("JSON schema 결과를 편집 가능한 markdown section 문서로 변환")
    void generate_success() {
        mockResponse("""
                {
                  "title":"8월 30일 회고",
                  "sections":[{
                    "id":"summary",
                    "title":"오늘의 요약",
                    "contentMd":"회고 API를 구현했다.",
                    "evidenceRefs":["activity:801"]
                  }]
                }
                """);

        var result = adapter.generate(
                project(), ReflectionKind.DAILY,
                LocalDate.of(2026, 8, 30), LocalDate.of(2026, 8, 30), source()
        );

        assertThat(result.title()).isEqualTo("8월 30일 회고");
        assertThat(result.content().sections()).hasSize(1);
        assertThat(result.content().sections().get(0).type()).isEqualTo("markdown");
    }

    @Test
    @DisplayName("snapshot에 없는 evidenceRef를 AI가 반환하면 생성 실패로 변환")
    void generate_unknownEvidenceRef() {
        mockResponse("""
                {
                  "title":"잘못된 회고",
                  "sections":[{
                    "id":"summary",
                    "title":"요약",
                    "contentMd":"근거 없는 내용",
                    "evidenceRefs":["activity:999"]
                  }]
                }
                """);

        assertThatThrownBy(() -> adapter.generate(
                project(), ReflectionKind.DAILY,
                LocalDate.of(2026, 8, 30), LocalDate.of(2026, 8, 30), source()
        )).isInstanceOf(ReflectionGenerationException.class)
                .hasFieldOrPropertyWithValue("errorCode", "REFLECTION_50001");
    }

    private void mockResponse(String json) {
        ChatResponse response = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        given(response.getResult().getOutput().getText()).willReturn(json);
        given(chatModel.call(any(Prompt.class))).willReturn(response);
    }

    private Project project() {
        return Project.builder()
                .id(101L).userId(1L).repoFullName("octocat/helpme").build();
    }

    private ReflectionSourceSnapshot source() {
        return new ReflectionSourceSnapshot(
                1, 0, List.of(new ReflectionSourceSnapshot.Evidence(
                "activity:801", "feat", "main · abc", "회고 API 구현"
        )), null, null, List.of(), 0, List.of(), false);
    }
}
