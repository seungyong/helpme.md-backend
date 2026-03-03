package seungyong.helpmebackend.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.user.domain.entity.JWTUser;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.domain.type.RedisKey;
import seungyong.helpmebackend.user.application.port.in.UserPortIn;
import seungyong.helpmebackend.global.application.port.out.JWTPortOut;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class UserService implements UserPortIn {
    private final RedisPortOut redisPortOut;
    private final JWTPortOut jwtPortOut;
    private final UserPortOut userPortOut;

    @Override
    public JWT reissue(String refreshToken) {
        Date now = new Date();

        if (jwtPortOut.isExpired(refreshToken, now)) {
            throw new CustomException(GlobalErrorCode.INVALID_TOKEN);
        }

        String refreshTokenKey = RedisKey.REFRESH_KEY.getValue() + refreshToken;
        String userId = redisPortOut.get(refreshTokenKey);

        if (userId == null) {
            throw new CustomException(GlobalErrorCode.INVALID_TOKEN);
        }

        Long userIdLong = Long.valueOf(userId);
        User user = userPortOut.getById(userIdLong);

        JWT jwt = jwtPortOut.generate(new JWTUser(userIdLong, user.getGithubUser().getName()));
        redisPortOut.set(refreshTokenKey, userId, jwt.getRefreshTokenExpireTime());

        return jwt;
    }

    @Override
    public void logout(Long userId, String refreshToken) {
        String refreshTokenKey = RedisKey.REFRESH_KEY.getValue() + refreshToken;
        redisPortOut.delete(refreshTokenKey);
    }

    @Override
    public void withdraw(Long userId, String refreshToken) {
        User user = userPortOut.getById(userId);
        userPortOut.delete(user);

        String refreshTokenKey = RedisKey.REFRESH_KEY.getValue() + refreshToken;
        redisPortOut.delete(refreshTokenKey);
    }
}
