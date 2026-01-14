package seungyong.helpmebackend.usecase.port.in.oauth2;

import seungyong.helpmebackend.infrastructure.jwt.JWT;

public interface OAuth2PortIn {
    JWT signupOrLogin(String code);
}
