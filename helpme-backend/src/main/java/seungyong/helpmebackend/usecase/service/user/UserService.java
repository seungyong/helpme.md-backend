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

        JWTUser user = jwtPortOut.getUserByTokenWithoutCheck(refreshToken);
        Long userId = user.getId();

        String refreshTokenKey = RedisKey.REFRESH_KEY.getValue() + userId;
        String storedRefreshToken = redisPortOut.get(refreshTokenKey);

        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new CustomException(GlobalErrorCode.INVALID_TOKEN);
        }

        JWT jwt = jwtPortOut.generate(new JWTUser(userId, user.getUsername()));
        redisPortOut.set(refreshTokenKey, jwt.getRefreshToken(), jwt.getRefreshTokenExpireTime());

        return jwt;
    }

    @Override
    public void withdraw(Long userId) {
        User user = userPortOut.getById(userId);
        userPortOut.delete(user);

        // Redis에 저장된 refresh token 삭제
        String refreshTokenKey = RedisKey.REFRESH_KEY.getValue() + userId;
        redisPortOut.delete(refreshTokenKey);
    }
}
