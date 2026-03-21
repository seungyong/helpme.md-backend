package seungyong.helpmebackend.section.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SectionTest {
    private final Long projectId = 1L;

    @Nested
    @DisplayName("splitContent - README 분할 테스트")
    class SplitContent {
        @Test
        @DisplayName("성공 - WHOLE")
        void splitContent_wholeMode() {
            String content = "# Title\n## Header\nBody";

            List<Section> sections = Section.splitContent(projectId, content, Section.SplitMode.WHOLE);

            assertThat(sections).hasSize(1);
            assertThat(sections.get(0).getTitle()).isEqualTo("Title");
            assertThat(sections.get(0).getContent()).isEqualTo(content);
            assertThat(sections.get(0).getOrderIdx()).isEqualTo(1);
        }

        @Test
        @DisplayName("성공 - SPLIT")
        void splitContent_splitMode() {
            String content = """
                    # Main Title
                    Intro text
                    ## Sub Title 1
                    Content 1
                    ## Sub Title 2
                    Content 2
                    """;

            List<Section> sections = Section.splitContent(projectId, content, Section.SplitMode.SPLIT);

            assertThat(sections).hasSize(3);
            assertThat(sections).extracting(Section::getTitle)
                    .containsExactly("Main Title", "Sub Title 1", "Sub Title 2");
            assertThat(sections).extracting(Section::getOrderIdx)
                    .containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("성공 - 코드 블록 제외")
        void splitContent_splitMode_withCodeBlock() {
            String content = """
                    # Header
                    ```java
                    // # This is a comment
                    public void main() {}
                    ```
                    ## Footer
                    """;

            List<Section> sections = Section.splitContent(projectId, content, Section.SplitMode.SPLIT);

            assertThat(sections).hasSize(2);
            assertThat(sections.get(0).getContent()).contains("# This is a comment");
            assertThat(sections.get(1).getTitle()).isEqualTo("Footer");
        }

        @Test
        @DisplayName("성공 - 제목 없는 콘텐츠")
        void splitContent_noTitle() {
            String content = "Just plain text without header";

            List<Section> sections = Section.splitContent(projectId, content, Section.SplitMode.SPLIT);

            assertThat(sections.get(0).getTitle()).isEqualTo(content);
            assertThat(sections.get(0).getContent()).isEqualTo(content);
        }

        @Test
        @DisplayName("성공 - 제목이 나중에 나오는 경우")
        void splitContent_titleLater() {
            String content = """
                    Intro text without header
                    ## Header
                    Content under header
                    """;

            List<Section> sections = Section.splitContent(projectId, content, Section.SplitMode.SPLIT);

            assertThat(sections).hasSize(2);
            assertThat(sections.get(0).getTitle()).isEqualTo("Intro text without header");
            assertThat(sections.get(1).getTitle()).isEqualTo("Header");
        }
    }

    @Nested
    @DisplayName("updateContent - 내용 수정")
    class UpdateContent {
        @Test
        @DisplayName("성공 - null 또는 빈 문자열")
        void updateContent_nullOrEmpty() {
            Section section = new Section(1L, projectId, "Title", "Old Content", 1);

            section.updateContent("");

            assertThat(section.getContent()).isEmpty();
        }

        @Test
        @DisplayName("성공 - 유효한 문자열")
        void updateContent_valid() {
            Section section = new Section(1L, projectId, "Title", "Old", 1);

            section.updateContent("New Content");

            assertThat(section.getContent()).isEqualTo("New Content");
        }
    }

    @Nested
    @DisplayName("updateOrderIdx - 정렬 순서 업데이트 테스트")
    class UpdateOrderIdx {
        @Test
        @DisplayName("성공: 유효한 양수가 들어오면 인덱스를 변경한다.")
        void updateOrderIdx_valid() {
            Section section = new Section(1L, projectId, "Title", "Content", 1);

            section.updateOrderIdx(5);

            assertThat(section.getOrderIdx()).isEqualTo(5);
        }

        @Test
        @DisplayName("실패 - null")
        void updateOrderIdx_null() {
            Section section = new Section(1L, projectId, "Title", "Content", 1);

            assertThatThrownBy(() -> section.updateOrderIdx(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("orderIdx cannot be null");
        }

        @Test
        @DisplayName("실패 - 음수")
        void updateOrderIdx_negative() {
            Section section = new Section(1L, projectId, "Title", "Content", 1);

            assertThatThrownBy(() -> section.updateOrderIdx(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("orderIdx cannot be negative");
        }
    }
}