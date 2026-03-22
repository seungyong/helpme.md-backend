package seungyong.helpmebackend.global.application.port.out;

import seungyong.helpmebackend.user.domain.entity.JWTUser;
import seungyong.helpmebackend.global.domain.entity.JWT;

import java.util.Date;

public interface JWTPortOut {
    JWT generate(JWTUser user);
    boolean isExpired(String token, Date current);
}
