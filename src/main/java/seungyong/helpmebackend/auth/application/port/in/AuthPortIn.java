package seungyong.helpmebackend.auth.application.port.in;

import seungyong.helpmebackend.auth.domain.entity.Installation;
import seungyong.helpmebackend.global.domain.entity.JWT;

import java.util.List;

public interface AuthPortIn {
    String generateLoginUrl();
    JWT signupOrLogin(String code, String state);
    List<Installation> getInstallations(Long userId);
}
