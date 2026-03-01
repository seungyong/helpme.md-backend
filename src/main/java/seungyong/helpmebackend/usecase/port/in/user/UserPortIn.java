package seungyong.helpmebackend.usecase.port.in.user;

import seungyong.helpmebackend.infrastructure.jwt.JWT;

public interface UserPortIn {
    JWT reissue(String refreshToken);
    void logout(Long userId, String refreshToken);
    void withdraw(Long userId, String refreshToken);
}
