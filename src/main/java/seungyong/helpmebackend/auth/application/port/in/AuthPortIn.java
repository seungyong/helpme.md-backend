package seungyong.helpmebackend.auth.application.port.in;

import seungyong.helpmebackend.global.domain.entity.JWT;

public interface AuthPortIn {
    String generateLoginUrl();
    JWT signupOrLogin(String code, String state);
}
