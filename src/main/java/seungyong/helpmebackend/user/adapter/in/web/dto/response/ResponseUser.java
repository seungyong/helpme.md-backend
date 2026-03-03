package seungyong.helpmebackend.user.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 정보 응답 DTO")
public record ResponseUser(
        @Schema(description = "사용자 이름", example = "helpme_user")
        String username
) {
}
