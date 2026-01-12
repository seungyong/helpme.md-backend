package seungyong.helpmebackend.usecase.port.in.github;

import seungyong.helpmebackend.infrastructure.jwt.JWT;

public interface GithubPortIn {
    JWT signupOrLogin(String code);
}
