package seungyong.helpmebackend.section.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import seungyong.helpmebackend.global.domain.entity.CustomUserDetails;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.DocumentErrorCode;
import seungyong.helpmebackend.global.filter.AuthenticationFilter;
import seungyong.helpmebackend.global.infrastructure.cookie.CookieUtil;
import seungyong.helpmebackend.section.application.port.in.ReadmeComponentPortIn;
import seungyong.helpmebackend.section.application.port.in.command.CreateReadmeComponentCommand;
import seungyong.helpmebackend.section.application.port.in.command.DeleteReadmeComponentCommand;
import seungyong.helpmebackend.section.application.port.in.command.UpdateReadmeComponentCommand;
import seungyong.helpmebackend.section.domain.entity.Section;
import seungyong.helpmebackend.support.config.TestSecurityConfig;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = ReadmeComponentController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AuthenticationFilter.class
        )
)
@Import(TestSecurityConfig.class)
class ReadmeComponentControllerTest {
    private static final Long USER_ID = 1L;
    private static final String OWNER = "octocat";
    private static final String NAME = "helpme-md";
    private static final String PATH = "/api/v1/repos/{owner}/{name}/components";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ReadmeComponentPortIn readmeComponentPortIn;
    @MockitoBean private CookieUtil cookieUtil;

    @Test
    @DisplayName("컴포넌트 목록은 orderIdx와 version·시각 메타데이터를 반환한다")
    void getComponents_success() throws Exception {
        given(readmeComponentPortIn.getComponents(USER_ID, OWNER, NAME))
                .willReturn(List.of(component(701L, 0, 4)));

        mockMvc.perform(get(PATH, OWNER, NAME).with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components[0].id").value(701L))
                .andExpect(jsonPath("$.components[0].orderIdx").value(0))
                .andExpect(jsonPath("$.components[0].version").value(4))
                .andExpect(jsonPath("$.components[0].createdAt")
                        .value("2026-08-30T10:00:00Z"));
    }

    @Test
    @DisplayName("저장된 컴포넌트가 없으면 200과 빈 배열을 반환한다")
    void getComponents_empty() throws Exception {
        given(readmeComponentPortIn.getComponents(USER_ID, OWNER, NAME))
                .willReturn(List.of());

        mockMvc.perform(get(PATH, OWNER, NAME).with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components").isEmpty());
    }

    @Test
    @DisplayName("컴포넌트 추가 요청을 command로 변환하고 Location을 반환한다")
    void createComponent_success() throws Exception {
        given(readmeComponentPortIn.createComponent(any(CreateReadmeComponentCommand.class)))
                .willReturn(component(707L, 1, 0));

        mockMvc.perform(post(PATH, OWNER, NAME).with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"트러블 슈팅","orderIdx":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/repos/octocat/helpme-md/components/707"
                ))
                .andExpect(jsonPath("$.version").value(0));

        ArgumentCaptor<CreateReadmeComponentCommand> captor =
                ArgumentCaptor.forClass(CreateReadmeComponentCommand.class);
        verify(readmeComponentPortIn).createComponent(captor.capture());
        assertThat(captor.getValue().content()).isNull();
        assertThat(captor.getValue().orderIdx()).isEqualTo(1);
    }

    @Test
    @DisplayName("컴포넌트 수정은 선택 필드와 필수 version을 전달한다")
    void updateComponent_success() throws Exception {
        given(readmeComponentPortIn.updateComponent(any(UpdateReadmeComponentCommand.class)))
                .willReturn(component(701L, 0, 5));

        mockMvc.perform(patch(PATH + "/{componentId}", OWNER, NAME, 701L)
                        .with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"새 본문","version":4}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(5))
                .andExpect(jsonPath("$.createdAt").doesNotExist());

        ArgumentCaptor<UpdateReadmeComponentCommand> captor =
                ArgumentCaptor.forClass(UpdateReadmeComponentCommand.class);
        verify(readmeComponentPortIn).updateComponent(captor.capture());
        assertThat(captor.getValue().componentId()).isEqualTo(701L);
        assertThat(captor.getValue().content()).isEqualTo("새 본문");
        assertThat(captor.getValue().version()).isEqualTo(4);
    }

    @Test
    @DisplayName("컴포넌트 삭제는 request body의 version을 전달하고 204를 반환한다")
    void deleteComponent_success() throws Exception {
        mockMvc.perform(delete(PATH + "/{componentId}", OWNER, NAME, 701L)
                        .with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":5}"))
                .andExpect(status().isNoContent());

        verify(readmeComponentPortIn).deleteComponent(
                new DeleteReadmeComponentCommand(
                        USER_ID, OWNER, NAME, 701L, 5
                )
        );
    }

    @Test
    @DisplayName("version 누락은 REQ_400이고 use case를 호출하지 않는다")
    void updateComponent_missingVersion() throws Exception {
        mockMvc.perform(patch(PATH + "/{componentId}", OWNER, NAME, 701L)
                        .with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"수정\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("REQ_400"));

        verify(readmeComponentPortIn, never())
                .updateComponent(any(UpdateReadmeComponentCommand.class));
    }

    @Test
    @DisplayName("version 충돌은 DOCUMENT_40901 응답")
    void updateComponent_versionConflict() throws Exception {
        given(readmeComponentPortIn.updateComponent(any(UpdateReadmeComponentCommand.class)))
                .willThrow(new CustomException(
                        DocumentErrorCode.DOCUMENT_VERSION_CONFLICT
                ));

        mockMvc.perform(patch(PATH + "/{componentId}", OWNER, NAME, 701L)
                        .with(user())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"오래된 수정\",\"version\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DOCUMENT_40901"));
    }

    private Section component(Long id, int orderIdx, int version) {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-30T10:00:00Z");
        return new Section(
                id,
                10L,
                "프로젝트 소개",
                "Helpme.md는 개발 기록 서비스입니다.",
                orderIdx,
                version,
                now,
                now
        );
    }

    private RequestPostProcessor user() {
        return SecurityMockMvcRequestPostProcessors.user(
                new CustomUserDetails(USER_ID, "octocat")
        );
    }
}
