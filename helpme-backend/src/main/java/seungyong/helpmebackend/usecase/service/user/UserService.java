package seungyong.helpmebackend.usecase.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.domain.entity.user.User;
import seungyong.helpmebackend.infrastructure.jwt.JWT;
import seungyong.helpmebackend.infrastructure.redis.RedisKey;
import seungyong.helpmebackend.usecase.port.in.user.UserPortIn;
import seungyong.helpmebackend.usecase.port.out.jwt.JWTPortOut;
import seungyong.helpmebackend.usecase.port.out.redis.RedisPortOut;
import seungyong.helpmebackend.usecase.port.out.user.UserPortOut;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

        Long userId = jwtPortOut.getUserIdByTokenWithoutCheck(refreshToken);

        String refreshTokenKey = RedisKey.REFRESH_KEY.getValue() + userId;
        String storedRefreshToken = redisPortOut.get(refreshTokenKey);

        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new CustomException(GlobalErrorCode.INVALID_TOKEN);
        }

        JWT jwt = jwtPortOut.generate(userId);
        LocalDateTime refreshTokenExpireTime = LocalDateTime.ofInstant(
                jwt.getRefreshTokenExpireTime(),
                ZoneOffset.UTC
        );
        redisPortOut.set(refreshTokenKey, jwt.getRefreshToken(), refreshTokenExpireTime);

        return jwt;
    }

    @Override
    public void withdraw(Long userId) {
        // TODO : Github App uninstall 처리

        User user = userPortOut.getById(userId);
        userPortOut.delete(user);

        // Redis에 저장된 refresh token 삭제
        String refreshTokenKey = RedisKey.REFRESH_KEY.getValue() + userId;
        redisPortOut.delete(refreshTokenKey);
    }
}
