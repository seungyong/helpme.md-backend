package seungyong.helpmebackend.usecase.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.domain.entity.user.JWTUser;
import seungyong.helpmebackend.domain.entity.user.User;
import seungyong.helpmebackend.infrastructure.jwt.JWT;
import seungyong.helpmebackend.infrastructure.redis.RedisKey;
import seungyong.helpmebackend.usecase.port.in.user.UserPortIn;
import seungyong.helpmebackend.usecase.port.out.jwt.JWTPortOut;
import seungyong.helpmebackend.usecase.port.out.redis.RedisPortOut;
import seungyong.helpmebackend.usecase.port.out.user.UserPortOut;

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
