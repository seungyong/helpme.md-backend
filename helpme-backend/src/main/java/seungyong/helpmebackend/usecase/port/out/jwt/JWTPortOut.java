package seungyong.helpmebackend.usecase.port.out.jwt;

import seungyong.helpmebackend.infrastructure.jwt.JWT;

public interface JWTPortOut {
    JWT generate(Long userId);
}
