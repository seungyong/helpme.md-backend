package seungyong.helpmebackend.section.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "섹션 내용 업데이트 요청 DTO")
public record RequestSectionContent(
        @Schema(description = "섹션 내용 (Markdown 형식)", example = "이 섹션은 프로젝트의 개요를 설명합니다.")
        @NotBlank(message = "섹션 내용은 필수입니다.")
        String content
) {
}
