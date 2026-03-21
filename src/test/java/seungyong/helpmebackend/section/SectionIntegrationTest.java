package seungyong.helpmebackend.section;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import seungyong.helpmebackend.global.config.SecurityConfig;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.infrastructure.github.GithubClient;
import seungyong.helpmebackend.global.infrastructure.jwt.JWTProvider;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;
import seungyong.helpmebackend.section.adapter.in.web.dto.request.RequestReorder;
import seungyong.helpmebackend.section.adapter.in.web.dto.request.RequestSection;
import seungyong.helpmebackend.section.application.port.out.SectionPortOut;
import seungyong.helpmebackend.section.domain.entity.Section;
import seungyong.helpmebackend.section.domain.exception.SectionErrorCode;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.GithubUser;
import seungyong.helpmebackend.user.domain.entity.JWTUser;
import seungyong.helpmebackend.user.domain.entity.User;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(SecurityConfig.class)
public class SectionIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private RedisTemplate<String, String> redisTemplate;
    @Autowired private JWTProvider jwtProvider;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private UserPortOut userPortOut;

    @MockitoSpyBean private SectionPortOut sectionPortOut;
    @MockitoSpyBean private ProjectPortOut projectPortOut;
    @MockitoSpyBean private CipherPortOut cipherPortOut;
    @MockitoBean private GithubClient githubClient;

    private final String OWNER = "test-user";
    private final String NAME = "test-project";
    private User user;
    private JWT jwt;

    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
            .defaultNotNull(true)
            .build();

    @BeforeEach
    void setup() {
        User newUser = new User(
                null,
                new GithubUser(
                        "test-user",
                        123456L,
                        new EncryptedToken("encrypted-token")
                )
        );
        user = userPortOut.save(newUser);

        jwt = jwtProvider.generate(new JWTUser(user.getId(), "test-user"));
        lenient().when(cipherPortOut.decrypt(any(String.class))).thenReturn("decrypted-token");

        String response = """
                {
                    "permission": "write"
                }
                """;
        lenient().when(githubClient.fetchGetMethodForBody(anyString(), anyString()))
                .thenReturn(response);

        String readmeResponse = """
                {
                    "content": "SGVsbG8sIHdvcmxkIQ==",
                    "sha": "readme-sha"
                }
                """;
        lenient().when(githubClient.fetchGetMethodForBody(anyString(), anyString(), anyString()))
                .thenReturn(readmeResponse);
    }

    @AfterEach
    void cleanup() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    private Project createProject() {
        return projectPortOut.save(new Project(
                null,
                user.getId(),
                OWNER + "/" + NAME
        ));
    }

    private Section createSection(Long projectId) {
        return sectionPortOut.save(new Section(
                null,
                projectId,
                "Test Section",
                "Test Description",
                1
        ));
    }

    @Nested
    @DisplayName("getSections - 섹션 목록 조회")
    class GetSections {
        @Test
        @DisplayName("성공")
        void success() throws Exception {
            Project project = createProject();
            Section section = createSection(project.getId());

            mockMvc.perform(get("/api/v1/repos/{owner}/{name}/sections", OWNER, NAME)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("sections[0].id").value(section.getId()))
                    .andExpect(jsonPath("sections[0].title").value(section.getTitle()))
                    .andExpect(jsonPath("sections[0].content").value(section.getContent()))
                    .andExpect(jsonPath("sections[0].orderIdx").value(section.getOrderIdx()))
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @DisplayName("실패 - 섹션이 없는 경우")
        void fail_notFoundSections() throws Exception {
            mockMvc.perform(get("/api/v1/repos/{owner}/{name}/sections", OWNER, NAME)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(SectionErrorCode.NOT_FOUND_SECTIONS.getErrorCode()))
                    .andDo(MockMvcResultHandlers.print());
        }
    }

    @Nested
    @DisplayName("createSection - 섹션 생성")
    class CreateSection {
        @Test
        @DisplayName("성공 - 프로젝트 있는 경우")
        void success() throws Exception {
            createProject();
            clearInvocations(projectPortOut);

            RequestSection request = fixtureMonkey.giveMeOne(RequestSection.class);

            mockMvc.perform(post("/api/v1/repos/{owner}/{name}/sections", OWNER, NAME)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            )
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/v1/repos/" + OWNER + "/" + NAME + "/sections")))
                    .andExpect(jsonPath("$.title").value(request.title()))
                    .andExpect(jsonPath("$.content").value(request.content()))
                    .andExpect(jsonPath("$.orderIdx").value(1))
                    .andDo(MockMvcResultHandlers.print());

            verify(projectPortOut, never()).save(any(Project.class));
        }

        @Test
        @DisplayName("성공 - 프로젝트 없는 경우")
        void success_noProject() throws Exception {
            clearInvocations(projectPortOut);
            RequestSection request = fixtureMonkey.giveMeOne(RequestSection.class);

            mockMvc.perform(post("/api/v1/repos/{owner}/{name}/sections", OWNER, NAME)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            )
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/v1/repos/" + OWNER + "/" + NAME + "/sections")))
                    .andExpect(jsonPath("$.title").value(request.title()))
                    .andExpect(jsonPath("$.content").value(request.content()))
                    .andExpect(jsonPath("$.orderIdx").value(1))
                    .andDo(MockMvcResultHandlers.print());

            verify(projectPortOut, times(1)).save(any(Project.class));
        }

        @Test
        @DisplayName("성공 - content가 없는 경우 (사용자 정의 섹션)")
        void success_noContent() throws Exception {
            createProject();

            RequestSection request = new RequestSection("Custom Section", null);

            mockMvc.perform(post("/api/v1/repos/{owner}/{name}/sections", OWNER, NAME)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            )
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/v1/repos/" + OWNER + "/" + NAME + "/sections")))
                    .andExpect(jsonPath("$.title").value(request.title()))
                    .andExpect(jsonPath("$.content").value("## " + request.title() + "\n\n"))
                    .andExpect(jsonPath("$.orderIdx").value(1))
                    .andDo(MockMvcResultHandlers.print());
        }
    }

    @Nested
    @DisplayName("initSections - 섹션 초기화")
    class InitSections {
        @Test
        @DisplayName("성공 - split 모드")
        void success_splitMode() throws Exception {
            mockMvc.perform(put("/api/v1/repos/{owner}/{name}/sections/init?branch=main&splitMode=split", OWNER, NAME)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sections").isArray())
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @DisplayName("성공 - whole 모드")
        void success_wholeMode() throws Exception {
            mockMvc.perform(put("/api/v1/repos/{owner}/{name}/sections/init?branch=main&splitMode=whole", OWNER, NAME)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/v1/repos/" + OWNER + "/" + NAME + "/sections")))
                    .andExpect(jsonPath("$.sections").isArray())
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @DisplayName("성공 - splitMode가 유효하지 않은 경우 whole 모드로 초기화")
        void success_invalidSplitMode() throws Exception {
            mockMvc.perform(put("/api/v1/repos/{owner}/{name}/sections/init?branch=main&splitMode=invalid", OWNER, NAME)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/v1/repos/" + OWNER + "/" + NAME + "/sections")))
                    .andExpect(jsonPath("$.sections").isArray())
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @DisplayName("성공 - 섹션이 있는 경우")
        void success_existingSections() throws Exception {
            Project project = createProject();
            createSection(project.getId());

            mockMvc.perform(put("/api/v1/repos/{owner}/{name}/sections/init?branch=main&splitMode=split", OWNER, NAME)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/v1/repos/" + OWNER + "/" + NAME + "/sections")))
                    .andExpect(jsonPath("$.sections").isArray())
                    .andDo(MockMvcResultHandlers.print());

            verify(sectionPortOut, times(1)).deleteAllByUserIdAndRepoFullName(anyLong(), anyString());
        }

        @Test
        @DisplayName("성공 - 프로젝트가 없는 경우")
        void success_noProject() throws Exception {
            clearInvocations(projectPortOut);
            RequestSection request = fixtureMonkey.giveMeOne(RequestSection.class);

            mockMvc.perform(put("/api/v1/repos/{owner}/{name}/sections/init?branch=main&splitMode=split", OWNER, NAME)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            )
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/v1/repos/" + OWNER + "/" + NAME + "/sections")))
                    .andExpect(jsonPath("$.sections").isArray())
                    .andDo(MockMvcResultHandlers.print());

            verify(projectPortOut, times(1)).save(any(Project.class));
        }

        @Test
        @DisplayName("성공 - README 내용이 없는 경우")
        void success_noReadmeContent() throws Exception {
            HttpClientErrorException notFoundException = HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null
            );
            lenient().when(githubClient.fetchGetMethodForBody(anyString(), anyString(), anyString()))
                    .thenThrow(notFoundException);

            mockMvc.perform(put("/api/v1/repos/{owner}/{name}/sections/init?branch=main&splitMode=split", OWNER, NAME)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/v1/repos/" + OWNER + "/" + NAME + "/sections")))
                    .andExpect(jsonPath("$.sections").isArray())
                    .andExpect(jsonPath("$.sections[0].title").value("Untitled Section"))
                    .andDo(MockMvcResultHandlers.print());
        }
    }

    @Nested
    @DisplayName("updateSectionContent - 섹션 내용 수정")
    class UpdateSectionContent {
        @Test
        @DisplayName("성공")
        void success() throws Exception {
            Project project = createProject();
            Section section = createSection(project.getId());

            String newContent = "Updated Content";
            mockMvc.perform(patch("/api/v1/repos/{owner}/{name}/sections/{sectionId}/content", OWNER, NAME, section.getId())
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            )
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(new RequestSection(section.getTitle(), newContent))))
                    .andExpect(status().isNoContent())
                    .andDo(MockMvcResultHandlers.print());

            mockMvc.perform(get("/api/v1/repos/{owner}/{name}/sections", OWNER, NAME)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("sections[0].id").value(section.getId()))
                    .andExpect(jsonPath("sections[0].content").value(newContent))
                    .andDo(MockMvcResultHandlers.print())
                    .andReturn();
        }

        @Test
        @DisplayName("실패 - 섹션이 없는 경우")
        void fail_notFoundSection() throws Exception {
            mockMvc.perform(patch("/api/v1/repos/{owner}/{name}/sections/{sectionId}/content", OWNER, NAME, 999L)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            )
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(new RequestSection("Title", "Content"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(SectionErrorCode.NOT_FOUND_SECTIONS.getErrorCode()))
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @DisplayName("실패 - content가 없는 경우")
        void fail_noContent() throws Exception {
            Project project = createProject();
            Section section = createSection(project.getId());

            mockMvc.perform(patch("/api/v1/repos/{owner}/{name}/sections/{sectionId}/content", OWNER, NAME, section.getId())
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            )
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(new RequestSection(section.getTitle(), null))))
                    .andExpect(status().isBadRequest())
                    .andDo(MockMvcResultHandlers.print());
        }
    }

    @Nested
    @DisplayName("reorderSections - 섹션 순서 변경")
    class reorderSections {
        @Test
        @DisplayName("reorderSections - 섹션 순서 변경")
        void success() throws Exception {
            Project project = createProject();
            Section section1 = createSection(project.getId());
            Section section2 = createSection(project.getId());

            RequestReorder request = new RequestReorder(List.of(section2.getId(), section1.getId()));
            mockMvc.perform(put("/api/v1/repos/{owner}/{name}/sections/reorder", OWNER, NAME)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            )
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent())
                    .andDo(MockMvcResultHandlers.print());

            mockMvc.perform(get("/api/v1/repos/{owner}/{name}/sections", OWNER, NAME)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("sections[0].id").value(section2.getId()))
                    .andExpect(jsonPath("sections[1].id").value(section1.getId()))
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @DisplayName("실패 - 섹션 ID 개수가 맞지 않음")
        void fail_invalidSectionIds() throws Exception {
            Project project = createProject();
            createSection(project.getId());
            createSection(project.getId());

            RequestReorder request = new RequestReorder(List.of(2L, 1L, 3L));
            mockMvc.perform(put("/api/v1/repos/{owner}/{name}/sections/reorder", OWNER, NAME)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            )
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(SectionErrorCode.INVALID_REORDER_REQUEST.getErrorCode()))
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 섹션 ID 포함")
        void fail_nonExistentSectionId() throws Exception {
            Project project = createProject();
            createSection(project.getId());
            createSection(project.getId());

            RequestReorder request = new RequestReorder(List.of(1L, 999L));
            mockMvc.perform(put("/api/v1/repos/{owner}/{name}/sections/reorder", OWNER, NAME)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            )
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(SectionErrorCode.NOT_FOUND_SECTIONS.getErrorCode()))
                    .andDo(MockMvcResultHandlers.print());
        }
    }

    @Nested
    @DisplayName("deleteSection - 섹션 삭제")
    class deleteSection {
        @Test
        @DisplayName("성공")
        void success() throws Exception {
            Project project = createProject();
            Section section1 = createSection(project.getId());
            Section section2 = createSection(project.getId());

            mockMvc.perform(delete("/api/v1/repos/{owner}/{name}/sections/{sectionId}", OWNER, NAME, section1.getId())
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andExpect(status().isNoContent())
                    .andDo(MockMvcResultHandlers.print());

            mockMvc.perform(get("/api/v1/repos/{owner}/{name}/sections", OWNER, NAME)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("sections[0].id").value(section2.getId()))
                    // orderIdx 1이 삭제되면서, 재정렬 발생
                    .andExpect(jsonPath("sections[0].orderIdx").value(1))
                    .andDo(MockMvcResultHandlers.print());
        }

        @Test
        @DisplayName("실패 - 섹션이 없는 경우")
        void fail_notFoundSection() throws Exception {
            mockMvc.perform(delete("/api/v1/repos/{owner}/{name}/sections/{sectionId}", OWNER, NAME, 999L)
                            .cookie(
                                    new Cookie("accessToken", jwt.getAccessToken()),
                                    new Cookie("refreshToken", jwt.getRefreshToken())
                            ))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(SectionErrorCode.NOT_FOUND_SECTIONS.getErrorCode()))
                    .andDo(MockMvcResultHandlers.print());
        }
    }
}