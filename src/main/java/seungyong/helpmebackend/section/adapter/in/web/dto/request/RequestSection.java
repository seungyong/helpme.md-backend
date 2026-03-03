package seungyong.helpmebackend.section.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "섹션 생성/수정 요청 DTO")
public record RequestSection(
        @Schema(description = "섹션 제목", example = "프로젝트 개요")
        @NotBlank(message = "Section 제목은 비어 있을 수 없습니다.")
        String title,
        @Schema(description = "섹션 내용 (Markdown 형식)", example = "이 섹션은 프로젝트의 개요를 설명합니다.")
        String content
) {
}
