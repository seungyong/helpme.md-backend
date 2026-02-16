package seungyong.helpmebackend.usecase.port.in.oauth2;

import seungyong.helpmebackend.adapter.in.web.dto.installation.response.ResponseInstallations;
import seungyong.helpmebackend.infrastructure.jwt.JWT;

public interface OAuth2PortIn {
    String generateLoginUrl();
    JWT signupOrLogin(String code, String state);
    ResponseInstallations getInstallations(Long userId);
}
