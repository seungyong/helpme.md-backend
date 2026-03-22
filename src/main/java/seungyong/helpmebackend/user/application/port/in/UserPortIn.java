package seungyong.helpmebackend.user.application.port.in;

import seungyong.helpmebackend.global.domain.entity.JWT;

public interface UserPortIn {
    JWT reissue(String refreshToken);
    void logout(Long userId, String refreshToken);
    void withdraw(Long userId, String refreshToken);
}
