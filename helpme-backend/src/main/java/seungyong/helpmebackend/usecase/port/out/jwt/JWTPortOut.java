package seungyong.helpmebackend.usecase.port.out.jwt;

import seungyong.helpmebackend.domain.entity.user.JWTUser;
import seungyong.helpmebackend.infrastructure.jwt.JWT;

import java.util.Date;

public interface JWTPortOut {
    JWT generate(JWTUser user);
    boolean isExpired(String token, Date current);
    JWTUser getUserByTokenWithoutCheck(String accessToken);
}
