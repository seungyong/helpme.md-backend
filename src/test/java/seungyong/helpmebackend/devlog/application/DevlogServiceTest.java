package seungyong.helpmebackend.devlog.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.devlog.application.port.in.command.SaveDevlogCommand;
import seungyong.helpmebackend.devlog.application.port.out.DevlogPortOut;
import seungyong.helpmebackend.devlog.domain.entity.Devlog;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.DocumentErrorCode;
import seungyong.helpmebackend.project.application.ProjectAccessResolver;
import seungyong.helpmebackend.project.domain.entity.Project;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DevlogServiceTest {
    private static final Long USER_ID = 1L;
    private static final Long PROJECT_ID = 101L;
    private static final LocalDate LOG_DATE = LocalDate.of(2026, 8, 23);

    @Mock private ProjectAccessResolver projectAccessResolver;
    @Mock private DevlogPortOut devlogPortOut;
    private DevlogService devlogService;

    @BeforeEach
    void setUp() {
        devlogService = new DevlogService(projectAccessResolver, devlogPortOut);
        given(projectAccessResolver.resolveActive(USER_ID, PROJECT_ID))
                .willReturn(Project.builder().id(PROJECT_ID).userId(USER_ID).build());
    }

    @Test
    @DisplayName("작성하지 않은 날짜 조회는 404 대신 빈 개발로그를 반환")
    void getDevlog_empty() {
        given(devlogPortOut.getByProjectIdAndLogDate(PROJECT_ID, LOG_DATE))
                .willReturn(Optional.empty());

        Devlog result = devlogService.getDevlog(USER_ID, PROJECT_ID, LOG_DATE);

        assertThat(result.exists()).isFalse();
        assertThat(result.projectId()).isEqualTo(PROJECT_ID);
        assertThat(result.logDate()).isEqualTo(LOG_DATE);
    }

    @Nested
    @DisplayName("개발로그 저장")
    class SaveDevlog {
        @Test
        @DisplayName("version이 null인 최초 요청은 version 0 개발로그를 생성")
        void create_success() {
            Devlog created = persisted("첫 개발로그", 0);
            given(devlogPortOut.getByProjectIdAndLogDate(PROJECT_ID, LOG_DATE))
                    .willReturn(Optional.empty());
            given(devlogPortOut.create(PROJECT_ID, LOG_DATE, "첫 개발로그"))
                    .willReturn(created);

            Devlog result = devlogService.saveDevlog(command("첫 개발로그", null));

            assertThat(result).isSameAs(created);
        }

        @Test
        @DisplayName("현재 version이 일치하면 내용을 수정하고 증가한 version을 반환")
        void update_success() {
            given(devlogPortOut.getByProjectIdAndLogDate(PROJECT_ID, LOG_DATE))
                    .willReturn(Optional.of(persisted("이전", 3)));
            given(devlogPortOut.updateIfVersionMatches(
                    eq(PROJECT_ID), eq(LOG_DATE), eq("수정"), eq(3), any()
            )).willReturn(Optional.of(persisted("수정", 4)));

            Devlog result = devlogService.saveDevlog(command("수정", 3));

            assertThat(result.contentMarkdown()).isEqualTo("수정");
            assertThat(result.version()).isEqualTo(4);
        }

        @Test
        @DisplayName("빈 내용은 현재 version이 일치할 때 기존 개발로그를 삭제")
        void delete_success() {
            given(devlogPortOut.getByProjectIdAndLogDate(PROJECT_ID, LOG_DATE))
                    .willReturn(Optional.of(persisted("기존", 2)));
            given(devlogPortOut.deleteIfVersionMatches(PROJECT_ID, LOG_DATE, 2))
                    .willReturn(true);

            Devlog result = devlogService.saveDevlog(command("   ", 2));

            assertThat(result.exists()).isFalse();
            verify(devlogPortOut).deleteIfVersionMatches(PROJECT_ID, LOG_DATE, 2);
        }

        @Test
        @DisplayName("작성하지 않은 날짜에 빈 내용을 저장하면 DB row를 만들지 않음")
        void emptyWithoutExisting_noOp() {
            given(devlogPortOut.getByProjectIdAndLogDate(PROJECT_ID, LOG_DATE))
                    .willReturn(Optional.empty());

            Devlog result = devlogService.saveDevlog(command("", null));

            assertThat(result.exists()).isFalse();
            verify(devlogPortOut, never()).create(any(), any(), any());
            verify(devlogPortOut, never()).deleteIfVersionMatches(any(), any(), any(Integer.class));
        }

        @Test
        @DisplayName("요청 version이 현재 version과 다르면 DOCUMENT_40901")
        void versionMismatch_conflict() {
            given(devlogPortOut.getByProjectIdAndLogDate(PROJECT_ID, LOG_DATE))
                    .willReturn(Optional.of(persisted("최신", 4)));

            assertVersionConflict(() -> devlogService.saveDevlog(command("오래된 수정", 3)));
            verify(devlogPortOut, never()).updateIfVersionMatches(
                    any(), any(), any(), any(Integer.class), any()
            );
        }

        @Test
        @DisplayName("조회 직후 다른 요청이 먼저 저장해 조건 UPDATE가 0건이면 DOCUMENT_40901")
        void concurrentUpdate_conflict() {
            given(devlogPortOut.getByProjectIdAndLogDate(PROJECT_ID, LOG_DATE))
                    .willReturn(Optional.of(persisted("조회 시점", 3)));
            given(devlogPortOut.updateIfVersionMatches(
                    eq(PROJECT_ID), eq(LOG_DATE), eq("내 수정"), eq(3), any()
            )).willReturn(Optional.empty());

            assertVersionConflict(() -> devlogService.saveDevlog(command("내 수정", 3)));
        }

        @Test
        @DisplayName("이미 삭제된 개발로그에 과거 version을 보내면 DOCUMENT_40901")
        void deletedByAnotherRequest_conflict() {
            given(devlogPortOut.getByProjectIdAndLogDate(PROJECT_ID, LOG_DATE))
                    .willReturn(Optional.empty());

            assertVersionConflict(() -> devlogService.saveDevlog(command("다시 저장", 2)));
        }
    }

    private SaveDevlogCommand command(String content, Integer version) {
        return new SaveDevlogCommand(USER_ID, PROJECT_ID, LOG_DATE, content, version);
    }

    private Devlog persisted(String content, int version) {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-23T10:00:00Z");
        return new Devlog(301L, PROJECT_ID, LOG_DATE, content, version, now, now);
    }

    private void assertVersionConflict(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", DocumentErrorCode.DOCUMENT_VERSION_CONFLICT
                );
    }
}
