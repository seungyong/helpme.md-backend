package seungyong.helpmebackend.section.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.DocumentErrorCode;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.section.application.port.out.SectionPortOut;
import seungyong.helpmebackend.section.domain.entity.Section;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReadmeComponentWriterTest {
    private static final Long USER_ID = 1L;
    private static final String FULL_NAME = "octocat/helpme-md";

    @Mock private SectionPortOut sectionPortOut;

    private ReadmeComponentWriter writer;

    @BeforeEach
    void setUp() {
        writer = new ReadmeComponentWriter(sectionPortOut);
        given(sectionPortOut.lockProject(10L)).willReturn(true);
    }

    @Test
    @DisplayName("orderIdx가 없으면 마지막 다음 위치에 version 0으로 생성한다")
    void create_append() {
        given(sectionPortOut.lastOrderIdxByUserIdAndRepoFullName(USER_ID, FULL_NAME))
                .willReturn(Optional.of(1));
        given(sectionPortOut.save(any(Section.class))).willAnswer(invocation -> {
            Section requested = invocation.getArgument(0);
            return new Section(
                    100L, requested.getProjectId(), requested.getTitle(),
                    requested.getContent(), requested.getOrderIdx()
            );
        });

        Section created = writer.create(project(), "트러블 슈팅", "본문", null);

        assertThat(created.getOrderIdx()).isEqualTo(2);
        assertThat(created.getVersion()).isZero();
        verify(sectionPortOut).increaseOrderIdxFrom(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.eq(FULL_NAME),
                org.mockito.ArgumentMatchers.eq(2),
                any(OffsetDateTime.class)
        );
    }

    @Test
    @DisplayName("중간 위치에 생성하면 기존 컴포넌트 순서를 먼저 이동한다")
    void create_insert() {
        given(sectionPortOut.lastOrderIdxByUserIdAndRepoFullName(USER_ID, FULL_NAME))
                .willReturn(Optional.of(2));
        given(sectionPortOut.save(any(Section.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Section created = writer.create(project(), "중간", "본문", 1);

        assertThat(created.getOrderIdx()).isEqualTo(1);
        verify(sectionPortOut).increaseOrderIdxFrom(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.eq(FULL_NAME),
                org.mockito.ArgumentMatchers.eq(1),
                any(OffsetDateTime.class)
        );
    }

    @Test
    @DisplayName("수정 시 순서 이동 후 요청 version과 일치하는 행만 갱신한다")
    void update_success() {
        Section current = section(100L, 1, 3);
        Section updated = section(100L, 0, 4);
        given(sectionPortOut.getByIdAndUserIdAndRepoFullName(100L, USER_ID, FULL_NAME))
                .willReturn(Optional.of(current));
        given(sectionPortOut.lastOrderIdxByUserIdAndRepoFullName(USER_ID, FULL_NAME))
                .willReturn(Optional.of(2));
        given(sectionPortOut.updateIfVersionMatches(
                org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.eq(FULL_NAME),
                org.mockito.ArgumentMatchers.eq("새 제목"),
                org.mockito.ArgumentMatchers.eq("기존 본문"),
                org.mockito.ArgumentMatchers.eq(0),
                org.mockito.ArgumentMatchers.eq(3),
                any(OffsetDateTime.class)
        )).willReturn(Optional.of(updated));

        Section result = writer.update(project(), 100L, "새 제목", null, 0, 3);

        assertThat(result.getVersion()).isEqualTo(4);
        verify(sectionPortOut).shiftOrderIdxForMove(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.eq(FULL_NAME),
                org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq(0),
                any(OffsetDateTime.class)
        );
    }

    @Test
    @DisplayName("조회한 version과 요청 version이 다르면 정렬을 변경하지 않는다")
    void update_staleVersion() {
        given(sectionPortOut.getByIdAndUserIdAndRepoFullName(100L, USER_ID, FULL_NAME))
                .willReturn(Optional.of(section(100L, 1, 4)));

        assertThatThrownBy(() -> writer.update(
                project(), 100L, "오래된 수정", null, 0, 3
        )).isInstanceOfSatisfying(CustomException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(DocumentErrorCode.DOCUMENT_VERSION_CONFLICT));

        verify(sectionPortOut, never()).shiftOrderIdxForMove(
                any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("삭제 성공 후 남은 컴포넌트 순서를 같은 작업에서 재정렬한다")
    void delete_success() {
        given(sectionPortOut.getByIdAndUserIdAndRepoFullName(100L, USER_ID, FULL_NAME))
                .willReturn(Optional.of(section(100L, 1, 2)));
        given(sectionPortOut.deleteIfVersionMatches(
                100L, USER_ID, FULL_NAME, 2
        )).willReturn(true);

        writer.delete(project(), 100L, 2);

        verify(sectionPortOut).decreaseOrderIdxAfterVersioned(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.eq(FULL_NAME),
                org.mockito.ArgumentMatchers.eq(1),
                any(OffsetDateTime.class)
        );
    }

    private Project project() {
        return Project.builder()
                .id(10L)
                .userId(USER_ID)
                .repoFullName(FULL_NAME)
                .githubInstallationId(20L)
                .githubRepoId(30L)
                .defaultBranch("main")
                .build();
    }

    private Section section(Long id, int orderIdx, int version) {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-30T10:00:00Z");
        return new Section(
                id, 10L, "기존 제목", "기존 본문", orderIdx, version, now, now
        );
    }
}
