package seungyong.helpmebackend.adapter.in.web.dto.user.response;

import java.time.LocalDateTime;

public record ResponseJWT(
        String grantType,
        String accessToken,
        String accessTokenExpireTime
) {
}