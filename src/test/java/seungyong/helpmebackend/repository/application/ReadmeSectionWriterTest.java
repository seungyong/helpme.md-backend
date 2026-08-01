package seungyong.helpmebackend.repository.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.repository.application.port.in.result.GeneratedReadmeResult;
import seungyong.helpmebackend.section.application.port.out.SectionPortOut;
import seungyong.helpmebackend.section.domain.entity.Section;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReadmeSectionWriterTest {
    @Mock private ProjectPortOut projectPortOut;
    @Mock private SectionPortOut sectionPortOut;

    private ReadmeSectionWriter writer;

    @BeforeEach
    void setUp() {
        writer = new ReadmeSectionWriter(projectPortOut, sectionPortOut);
    }

    @Test
    @DisplayName("기존 section을 교체하고 저장 결과를 application result로 반환한다")
    void replace_existingSections() {
        String repo = "owner/repo";
        Project project = new Project(10L, 1L, repo);
        given(projectPortOut.getByUserIdAndRepoFullName(1L, repo)).willReturn(Optional.of(project));
        given(sectionPortOut.getSectionsByUserIdAndRepoFullName(1L, repo))
                .willReturn(List.of(new Section(1L, 10L, "Old", "old", 1)));
        given(sectionPortOut.saveAll(anyList())).willReturn(List.of(
                new Section(2L, 10L, "Overview", "# Overview", 1),
                new Section(3L, 10L, "Usage", "## Usage", 2)
        ));

        GeneratedReadmeResult result = writer.replace(
                1L, repo, "# Overview\ncontent\n## Usage\nexample"
        );

        verify(sectionPortOut).deleteAllByUserIdAndRepoFullName(1L, repo);
        assertThat(result.sections()).extracting(GeneratedReadmeResult.Section::title)
                .containsExactly("Overview", "Usage");
    }

    @Test
    @DisplayName("프로젝트가 없으면 생성하고 비어 있는 section 삭제는 생략한다")
    void replace_createsProject() {
        String repo = "owner/repo";
        Project savedProject = new Project(10L, 1L, repo);
        given(projectPortOut.getByUserIdAndRepoFullName(1L, repo)).willReturn(Optional.empty());
        given(projectPortOut.save(any(Project.class))).willReturn(savedProject);
        given(sectionPortOut.getSectionsByUserIdAndRepoFullName(1L, repo)).willReturn(List.of());
        given(sectionPortOut.saveAll(anyList())).willReturn(List.of(
                new Section(1L, 10L, "README", "# README", 1)
        ));

        GeneratedReadmeResult result = writer.replace(1L, repo, "# README");

        verify(sectionPortOut, never()).deleteAllByUserIdAndRepoFullName(1L, repo);
        assertThat(result.sections()).hasSize(1);
    }
}
