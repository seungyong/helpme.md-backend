package seungyong.helpmebackend.section.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.section.application.port.in.command.CreateReadmeComponentCommand;
import seungyong.helpmebackend.section.application.port.in.command.UpdateReadmeComponentCommand;
import seungyong.helpmebackend.section.application.port.out.SectionPortOut;
import seungyong.helpmebackend.section.domain.entity.Section;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReadmeComponentServiceTest {
    private static final Long USER_ID = 1L;
    private static final String OWNER = "octocat";
    private static final String NAME = "helpme-md";

    @Mock private ReadmeComponentRepositoryAccessResolver repositoryAccessResolver;
    @Mock private ReadmeComponentWriter componentWriter;
    @Mock private SectionPortOut sectionPortOut;

    private ReadmeComponentService service;

    @BeforeEach
    void setUp() {
        service = new ReadmeComponentService(
                repositoryAccessResolver, componentWriter, sectionPortOut
        );
    }

    @Test
    @DisplayName("저장된 컴포넌트가 없으면 빈 목록을 반환한다")
    void getComponents_empty() {
        given(repositoryAccessResolver.resolveWritable(USER_ID, OWNER, NAME))
                .willReturn(project());
        given(sectionPortOut.getSectionsByUserIdAndRepoFullName(
                USER_ID, OWNER + "/" + NAME
        )).willReturn(List.of());

        assertThat(service.getComponents(USER_ID, OWNER, NAME)).isEmpty();
    }

    @Test
    @DisplayName("컴포넌트 생성 시 제목을 정리하고 누락된 content를 빈 문자열로 전달한다")
    void createComponent_defaults() {
        Project project = project();
        Section saved = new Section(100L, 10L, "소개", "", 0);
        given(repositoryAccessResolver.resolveWritable(USER_ID, OWNER, NAME))
                .willReturn(project);
        given(componentWriter.create(project, "소개", "", null)).willReturn(saved);

        Section result = service.createComponent(new CreateReadmeComponentCommand(
                USER_ID, OWNER, NAME, "  소개  ", null, null
        ));

        assertThat(result).isSameAs(saved);
    }

    @Test
    @DisplayName("공백 제목은 외부 권한 확인 전에 REQ_400으로 거절한다")
    void createComponent_blankTitle() {
        CreateReadmeComponentCommand command = new CreateReadmeComponentCommand(
                USER_ID, OWNER, NAME, "  ", null, null
        );

        assertThatThrownBy(() -> service.createComponent(command))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(GlobalErrorCode.BAD_REQUEST));
        verify(repositoryAccessResolver, never())
                .resolveWritable(any(), any(), any());
    }

    @Test
    @DisplayName("수정 요청의 version과 변경 필드를 writer에 그대로 전달한다")
    void updateComponent_success() {
        Project project = project();
        Section updated = new Section(100L, 10L, "새 제목", "본문", 0);
        given(repositoryAccessResolver.resolveWritable(USER_ID, OWNER, NAME))
                .willReturn(project);
        given(componentWriter.update(project, 100L, "새 제목", null, 0, 3))
                .willReturn(updated);

        Section result = service.updateComponent(new UpdateReadmeComponentCommand(
                USER_ID, OWNER, NAME, 100L, " 새 제목 ", null, 0, 3
        ));

        assertThat(result).isSameAs(updated);
    }

    private Project project() {
        return Project.builder()
                .id(10L)
                .userId(USER_ID)
                .repoFullName(OWNER + "/" + NAME)
                .githubInstallationId(20L)
                .githubRepoId(30L)
                .defaultBranch("main")
                .build();
    }
}
