package seungyong.helpmebackend.adapter.in.web.dto.section.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RequestReorder(
        @Schema(description = "재정렬할 섹션 ID 목록 (앞에 있는 ID가 더 앞에 위치하게 됩니다.)", example = "[1, 2, 3]")
        @NotEmpty(message = "섹션 ID 목록은 비어 있을 수 없습니다.")
        List<Long> sectionIds
) {
}
