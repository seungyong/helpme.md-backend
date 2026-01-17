package seungyong.helpmebackend.adapter.in.web.dto.repository.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "리포지토리 평가 응답 DTO")
public record ResponseEvaluation(
        @Schema(description = "평가 점수", example = "4.5")
        Float rating,
        @Schema(description = "평가 내용", example = "[\"전체적으로 잘 작성되었습니다.\", \"설치 방법을 추가하면 좋겠습니다.\"]" )
        List<String> contents
) {
}
