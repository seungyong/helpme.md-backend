package seungyong.helpmebackend.usecase.port.out.jwt;

import seungyong.helpmebackend.infrastructure.jwt.JWT;

import java.util.Date;

public interface JWTPortOut {
    JWT generate(Long userId);
    boolean isExpired(String token, Date current);
    Long getUserIdByAccessTokenWithoutCheck(String accessToken);
}
