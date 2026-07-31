package seungyong.helpmebackend.user.application.port.in;

import seungyong.helpmebackend.global.domain.entity.JWT;

public interface UserPortIn {
    void ensureActiveUser(Long userId);
    JWT reissue(String refreshToken);
    void logout(String refreshToken);
    void withdraw(Long userId, String refreshToken);
}
