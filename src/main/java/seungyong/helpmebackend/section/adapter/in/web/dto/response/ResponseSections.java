package seungyong.helpmebackend.section.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "섹션 목록 응답 DTO")
public record ResponseSections(
        @Schema(
                description = "섹션 목록",
                example = """
                        [
                          {
                            "id": 1,
                            "title": "프로젝트 개요",
                            "content": "이 섹션은 프로젝트의 개요를 설명합니다.",
                            "orderIdx": 1
                          },
                          {
                            "id": 2,
                            "title": "설치 방법",
                            "content": "이 섹션은 프로젝트의 설치 방법을 설명합니다.",
                            "orderIdx": 2
                          }
                        ]
                        """
        )
        List<Section> sections
) {
    @Schema(description = "섹션 정보")
    public record Section(
            @Schema(description = "섹션 ID", example = "1")
            Long id,
            @Schema(description = "섹션 제목", example = "프로젝트 개요")
            String title,
            @Schema(description = "섹션 내용 (Markdown 형식)", example = "이 섹션은 프로젝트의 개요를 설명합니다.")
            String content,
            @Schema(description = "섹션 순서 (1부터 시작)", example = "1")
            Integer orderIdx
    ) { }
}
