package seungyong.helpmebackend.section.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.repository.application.port.out.RepositoryPortOut;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;
import seungyong.helpmebackend.repository.domain.exception.RepositoryErrorCode;
import seungyong.helpmebackend.section.adapter.in.web.dto.request.RequestReorder;
import seungyong.helpmebackend.section.adapter.in.web.dto.request.RequestSectionContent;
import seungyong.helpmebackend.section.adapter.in.web.dto.response.ResponseSections;
import seungyong.helpmebackend.section.application.port.out.SectionPortOut;
import seungyong.helpmebackend.section.domain.entity.Section;
import seungyong.helpmebackend.section.domain.exception.SectionErrorCode;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.GithubUser;
import seungyong.helpmebackend.user.domain.entity.User;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {
    @InjectMocks private SectionService sectionService;

    @Mock private UserPortOut userPortOut;
    @Mock private ProjectPortOut projectPortOut;
    @Mock private SectionPortOut sectionPortOut;
    @Mock private RepositoryPortOut repositoryPortOut;
    @Mock private CipherPortOut cipherPortOut;
    @Mock private RedisPortOut redisPortOut;

    private final Long USER_ID = 1L;
    private final String OWNER = "test-owner";
    private final String REPO = "test-repo";
    private final String FULL_NAME = OWNER + "/" + REPO;
    private User user;

    @BeforeEach
    void setup() {
        // 기본 유저 세팅 (권한 체크 로직에서 사용)
        user = new User(USER_ID, new GithubUser("test-user", 123456L, new EncryptedToken("enc-token")));
    }

    @Nested
    @DisplayName("getSections - 섹션 목록 조회")
    class GetSections {
        @Test
        @DisplayName("성공")
        void getSections_success() {
            mockAuthCheck(true);

            List<Section> sections = List.of(
                    new Section(1L, 100L, "Title 1", "Content 1", 1),
                    new Section(2L, 100L, "Title 2", "Content 2", 2)
            );
            given(sectionPortOut.getSectionsByUserIdAndRepoFullName(USER_ID, FULL_NAME)).willReturn(sections);

            ResponseSections result = sectionService.getSections(USER_ID, OWNER, REPO);

            assertThat(result.sections()).hasSize(2);
            assertThat(result.sections().get(0).title()).isEqualTo("Title 1");
        }

        @Test
        @DisplayName("실패 - 섹션 없음")
        void getSections_notFound() {
            mockAuthCheck(true);

            given(sectionPortOut.getSectionsByUserIdAndRepoFullName(USER_ID, FULL_NAME)).willReturn(List.of());

            assertThatThrownBy(() -> sectionService.getSections(USER_ID, OWNER, REPO))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", SectionErrorCode.NOT_FOUND_SECTIONS);
        }
    }

    @Nested
    @DisplayName("initSections - 섹션 초기화")
    class InitSections {
        @Test
        @DisplayName("성공")
        void initSections_success_split() {
            mockAuthCheck(true);

            given(cipherPortOut.decrypt(anyString())).willReturn("decrypted-token");
            given(projectPortOut.getByUserIdAndRepoFullName(USER_ID, FULL_NAME))
                    .willReturn(Optional.of(new Project(100L, USER_ID, FULL_NAME)));

            String readme = "# Header\nContent\n## Sub\nMore content";
            given(repositoryPortOut.getReadmeContent(any())).willReturn(readme);

            // 기존 섹션 존재 상황
            given(sectionPortOut.getSectionsByUserIdAndRepoFullName(USER_ID, FULL_NAME))
                    .willReturn(List.of(new Section(1L, 100L, "Old", "Old", 1)));

            List<Section> splitSections = List.of(new Section(10L, 100L, "Header", "Content", 1));
            given(sectionPortOut.saveAll(anyList())).willReturn(splitSections);

            ResponseSections result = sectionService.initSections(USER_ID, OWNER, REPO, "main", "split");

            verify(sectionPortOut).deleteAllByUserIdAndRepoFullName(USER_ID, FULL_NAME);
            assertThat(result.sections()).hasSize(1);
            assertThat(result.sections().get(0).title()).isEqualTo("Header");
        }

        @Test
        @DisplayName("성공 - README 내용 없음")
        void initSections_success_noReadme() {
            mockAuthCheck(true);

            given(cipherPortOut.decrypt(anyString())).willReturn("decrypted-token");
            given(projectPortOut.getByUserIdAndRepoFullName(USER_ID, FULL_NAME))
                    .willReturn(Optional.of(new Project(100L, USER_ID, FULL_NAME)));

            List<Section> sections = List.of(new Section(1L, 100L, "Old", "Old", 1));
            given(sectionPortOut.getSectionsByUserIdAndRepoFullName(USER_ID, FULL_NAME))
                    .willReturn(sections);

            given(repositoryPortOut.getReadmeContent(any())).willReturn("");

            given(sectionPortOut.save(any(Section.class)))
                    .willReturn(new Section(10L, 100L, "Untitled Section", "", 1));

            ResponseSections result = sectionService.initSections(USER_ID, OWNER, REPO, "main", "split");

            verify(sectionPortOut).deleteAllByUserIdAndRepoFullName(USER_ID, FULL_NAME);
            verify(sectionPortOut, times(1)).save(any(Section.class));

            assertThat(result.sections()).hasSize(1);
            assertThat(result.sections().get(0).title()).isEqualTo("Untitled Section");
        }

        @Test
        @DisplayName("성공 - 프로젝트 없음")
        void initSections_success_noProject() {
            mockAuthCheck(true);

            given(cipherPortOut.decrypt(anyString())).willReturn("decrypted-token");
            given(projectPortOut.getByUserIdAndRepoFullName(USER_ID, FULL_NAME))
                    .willReturn(Optional.empty());
            given(projectPortOut.save(any(Project.class)))
                    .willReturn(new Project(100L, USER_ID, FULL_NAME));

            String readme = "# Header\nContent";
            given(repositoryPortOut.getReadmeContent(any())).willReturn(readme);
            given(sectionPortOut.saveAll(anyList()))
                    .willReturn(List.of(new Section(10L, 100L, "Header", "Content", 1)));

            ResponseSections result = sectionService.initSections(USER_ID, OWNER, REPO, "main", "split");

            verify(projectPortOut).save(any(Project.class));
            assertThat(result.sections()).hasSize(1);
            assertThat(result.sections().get(0).title()).isEqualTo("Header");
        }
    }

    @Nested
    @DisplayName("updateSectionContent - 섹션 내용 수정")
    class UpdateSectionContent {
        @Test
        @DisplayName("성공")
        void updateSectionContent_success() {
            mockAuthCheck(true);

            Section target = new Section(10L, 100L, "Old Title", "Old Content", 1);
            given(sectionPortOut.getByIdAndUserId(10L, USER_ID)).willReturn(Optional.of(target));

            sectionService.updateSectionContent(USER_ID, OWNER, REPO, 10L, new RequestSectionContent("New Content"));

            assertThat(target.getContent()).isEqualTo("New Content");
            verify(sectionPortOut).save(target);
        }

        @Test
        @DisplayName("실패 - 섹션 없음")
        void updateSectionContent_notFound() {
            mockAuthCheck(true);

            given(sectionPortOut.getByIdAndUserId(10L, USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sectionService.updateSectionContent(USER_ID, OWNER, REPO, 10L, new RequestSectionContent("New Content")))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", SectionErrorCode.NOT_FOUND_SECTIONS);
        }
    }

    @Nested
    @DisplayName("reorderSections - 순서 변경")
    class ReorderSections {
        @Test
        @DisplayName("성공")
        void reorderSections_success() {
            mockAuthCheck(true);

            Section s1 = new Section(1L, 100L, "T1", "C1", 1);
            Section s2 = new Section(2L, 100L, "T2", "C2", 2);
            given(sectionPortOut.getSectionsByUserIdAndRepoFullName(USER_ID, FULL_NAME)).willReturn(List.of(s1, s2));

            // 2번을 첫 번째로
            RequestReorder request = new RequestReorder(List.of(2L, 1L));

            sectionService.reorderSections(USER_ID, OWNER, REPO, request);

            assertThat(s2.getOrderIdx()).isEqualTo(1);
            assertThat(s1.getOrderIdx()).isEqualTo(2);

            verify(sectionPortOut).saveAll(anyList());
        }

        @Test
        @DisplayName("실패 - 섹션 ID 리스트 크기 불일치")
        void reorderSections_invalidSize() {
            mockAuthCheck(true);

            given(sectionPortOut.getSectionsByUserIdAndRepoFullName(USER_ID, FULL_NAME)).willReturn(List.of(
                    new Section(1L, 100L, "T", "C", 1),
                    new Section(2L, 100L, "T", "C", 2)
            ));
            RequestReorder request = new RequestReorder(List.of(1L, 2L, 3L));

            assertThatThrownBy(() -> sectionService.reorderSections(USER_ID, OWNER, REPO, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", SectionErrorCode.INVALID_REORDER_REQUEST);
        }
    }

    @Nested
    @DisplayName("deleteSection - 섹션 삭제")
    class DeleteSection {
        @Test
        @DisplayName("성공")
        void deleteSection_success() {
            mockAuthCheck(true);

            Section target = new Section(10L, 100L, "Del", "Content", 2);
            given(sectionPortOut.getByIdAndUserId(10L, USER_ID)).willReturn(Optional.of(target));

            sectionService.deleteSection(USER_ID, OWNER, REPO, 10L);

            verify(sectionPortOut).decreaseOrderIdxAfter(USER_ID, FULL_NAME, 2);
            verify(sectionPortOut).delete(target);
        }

        @Test
        @DisplayName("실패 - 섹션 없음")
        void deleteSection_notFound() {
            mockAuthCheck(true);

            given(sectionPortOut.getByIdAndUserId(10L, USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sectionService.deleteSection(USER_ID, OWNER, REPO, 10L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", SectionErrorCode.NOT_FOUND_SECTIONS);
        }
    }

    @Test
    @DisplayName("권한 체크 실패")
    void authCheck_fail() {
        mockAuthCheck(false);

        assertThatThrownBy(() -> sectionService.getSections(USER_ID, OWNER, REPO))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", RepositoryErrorCode.REPOSITORY_FORBIDDEN);
    }

    /**
     * 공통 권한 체크 로직 모킹
     */
    private void mockAuthCheck(boolean isAuthorized) {
        given(userPortOut.getById(USER_ID)).willReturn(user);

        // Redis에 권한 정보가 없는 상태 가정 (API 호출 유도)
        given(redisPortOut.exists(anyString())).willReturn(false);

        given(cipherPortOut.decrypt(anyString())).willReturn("decrypted-token");
        given(repositoryPortOut.checkPermission(any())).willReturn(isAuthorized);
    }
}