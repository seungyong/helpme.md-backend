package seungyong.helpmebackend.section.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.filter.AuthenticationFilter;
import seungyong.helpmebackend.global.infrastructure.cookie.CookieUtil;
import seungyong.helpmebackend.global.infrastructure.jwt.JWTProvider;
import seungyong.helpmebackend.section.adapter.in.web.dto.request.RequestReorder;
import seungyong.helpmebackend.section.adapter.in.web.dto.request.RequestSection;
import seungyong.helpmebackend.section.adapter.in.web.dto.request.RequestSectionContent;
import seungyong.helpmebackend.section.adapter.in.web.dto.response.ResponseSections;
import seungyong.helpmebackend.section.application.port.in.SectionPortIn;
import seungyong.helpmebackend.support.config.TestSecurityConfig;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = SectionController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AuthenticationFilter.class
        )
)
@Import(TestSecurityConfig.class)
public class SectionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private SectionPortIn sectionPortIn;
    @MockitoBean private JWTProvider jwtProvider;
    @MockitoBean private CookieUtil cookieUtil;

    private final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
            .defaultNotNull(true)
            .build();

    private final CustomUserDetails userDetails = new CustomUserDetails(1L, "test-user");

    @Nested
    @DisplayName("getSections - 섹션 목록 조회")
    class GetSections {
        @Test
        @DisplayName("성공 - 섹션 목록을 정상적으로 불러올 때")
        void getSections_success() throws Exception {
            ResponseSections response = fixtureMonkey.giveMeBuilder(ResponseSections.class)
                    .set("sections", List.of(
                            new ResponseSections.Section(1L, "Section 1", "Content 1", 1),
                            new ResponseSections.Section(2L, "Section 2", "Content 2", 2)
                    ))
                    .sample();
            given(sectionPortIn.getSections(anyLong(), anyString(), anyString())).willReturn(response);

            mockMvc.perform(get("/api/v1/repos/{owner}/{name}/sections", "owner", "repo")
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sections[0].title").value(response.sections().get(0).title()));
        }
    }

    @Nested
    @DisplayName("createSection - 새로운 섹션 생성")
    class CreateSection {
        @Test
        @DisplayName("성공")
        void createSection_success() throws Exception {
            RequestSection request = new RequestSection("New Title", "New Content");
            ResponseSections.Section response = new ResponseSections.Section(1L, "New Title", "New Content", 1);

            given(sectionPortIn.createSection(anyLong(), anyString(), anyString(), any(RequestSection.class)))
                    .willReturn(response);

            mockMvc.perform(post("/api/v1/repos/{owner}/{name}/sections", "owner", "repo")
                            .with(user(userDetails))
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("New Title"))
                    .andExpect(header().string("Location", containsString("/sections")));
        }

        @Test
        @DisplayName("성공 - 빈 내용")
        void createSection_emptyContent() throws Exception {
            RequestSection request = new RequestSection("New Title", "");

            ResponseSections.Section response = new ResponseSections.Section(1L, "New Title", "", 1);
            given(sectionPortIn.createSection(anyLong(), anyString(), anyString(), any(RequestSection.class)))
                    .willReturn(response);

            mockMvc.perform(post("/api/v1/repos/{owner}/{name}/sections", "owner", "repo")
                            .with(user(userDetails))
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("New Title"))
                    .andExpect(jsonPath("$.content").value(""));
        }

        @Test
        @DisplayName("실패 - 빈 제목")
        void createSection_badRequest() throws Exception {
            RequestSection request = new RequestSection("", "Content");

            mockMvc.perform(post("/api/v1/repos/{owner}/{name}/sections", "owner", "repo")
                            .with(user(userDetails))
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("initSections - 섹션 초기화")
    class InitSections {
        @Test
        @DisplayName("성공")
        void initSections_success() throws Exception {
            ResponseSections response = fixtureMonkey.giveMeOne(ResponseSections.class);
            given(sectionPortIn.initSections(anyLong(), anyString(), anyString(), anyString(), anyString()))
                    .willReturn(response);

            mockMvc.perform(put("/api/v1/repos/{owner}/{name}/sections/init", "owner", "repo")
                            .with(user(userDetails))
                            .param("branch", "main")
                            .param("splitMode", "WHOLE"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sections").isArray())
                    .andExpect(header().string("Location", containsString("/sections")));
        }
    }

    @Nested
    @DisplayName("updateSectionContent - 섹션 내용 수정")
    class UpdateSectionContent {
        @Test
        @DisplayName("성공")
        void updateSectionContent_success() throws Exception {
            RequestSectionContent request = new RequestSectionContent("Updated Content");

            doNothing().when(sectionPortIn).updateSectionContent(anyLong(), anyString(), anyString(), anyLong(), any(RequestSectionContent.class));

            mockMvc.perform(patch("/api/v1/repos/{owner}/{name}/sections/{sectionId}/content", "owner", "repo", 1L)
                            .with(user(userDetails))
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("실패 - sectionId 없음")
        void updateSectionContent_badRequest() throws Exception {
            RequestSectionContent request = new RequestSectionContent("Updated Content");

            mockMvc.perform(patch("/api/v1/repos/{owner}/{name}/sections/{sectionId}/content", "owner", "repo", null)
                            .with(user(userDetails))
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - content 없음")
        void updateSectionContent_badRequest2() throws Exception {
            RequestSectionContent request = new RequestSectionContent("");

            mockMvc.perform(patch("/api/v1/repos/{owner}/{name}/sections/{sectionId}/content", "owner", "repo", 1L)
                            .with(user(userDetails))
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("reorderSection")
    class ReorderSection {
        @Test
        @DisplayName("성공")
        void reorderSection_success() throws Exception {
            RequestReorder request = new RequestReorder(List.of(1L, 2L));

            doNothing().when(sectionPortIn).reorderSections(anyLong(), anyString(), anyString(), any(RequestReorder.class));

            mockMvc.perform(put("/api/v1/repos/{owner}/{name}/sections/reorder", "owner", "repo")
                            .with(user(userDetails))
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("실패 - 빈 섹션 ID 목록")
        void reorderSection_badRequest() throws Exception {
            RequestReorder request = new RequestReorder(List.of());

            mockMvc.perform(put("/api/v1/repos/{owner}/{name}/sections/reorder", "owner", "repo")
                            .with(user(userDetails))
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("deleteSection")
    class DeleteSection {
        @Test
        @DisplayName("성공")
        void deleteSection_success() throws Exception {
            doNothing().when(sectionPortIn).deleteSection(anyLong(), anyString(), anyString(), anyLong());

            mockMvc.perform(delete("/api/v1/repos/{owner}/{name}/sections/{sectionId}", "owner", "name", 1L)
                            .with(user(userDetails)))
                    .andExpect(status().isNoContent());
        }
    }
}