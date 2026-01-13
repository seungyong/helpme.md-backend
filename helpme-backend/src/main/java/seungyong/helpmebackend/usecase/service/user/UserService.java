package seungyong.helpmebackend.usecase.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import seungyong.helpmebackend.domain.entity.user.User;
import seungyong.helpmebackend.infrastructure.redis.RedisKey;
import seungyong.helpmebackend.usecase.port.in.user.UserPortIn;
import seungyong.helpmebackend.usecase.port.out.redis.RedisPortOut;
import seungyong.helpmebackend.usecase.port.out.user.UserPortOut;

@Service
@RequiredArgsConstructor
public class UserService implements UserPortIn {
    private final RedisPortOut redisPortOut;
    private final UserPortOut userPortOut;

    @Override
    public void withdraw(Long userId) {
        User user = userPortOut.getById(userId);
        userPortOut.delete(user);

        // Redis에 저장된 refresh token 삭제
        String refreshTokenKey = RedisKey.REFRESH_KEY.getValue() + userId;
        redisPortOut.delete(refreshTokenKey);
    }
}
