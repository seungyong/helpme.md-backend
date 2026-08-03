package seungyong.helpmebackend.user.application.port.in;

import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.user.domain.entity.User;

public interface UserPortIn {
    void ensureActiveUser(Long userId);
    User getCurrentUser(Long userId);
    JWT reissue(String refreshToken);
    void logout(String refreshToken);
    void withdraw(Long userId, String refreshToken);
}
