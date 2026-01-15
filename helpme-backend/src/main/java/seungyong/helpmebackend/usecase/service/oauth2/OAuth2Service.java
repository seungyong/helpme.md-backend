package seungyong.helpmebackend.usecase.service.oauth2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.adapter.out.persistence.mapper.UserPortOutMapper;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.domain.entity.user.GithubUser;
import seungyong.helpmebackend.domain.entity.user.User;
import seungyong.helpmebackend.infrastructure.jwt.JWT;
import seungyong.helpmebackend.infrastructure.redis.RedisKey;
import seungyong.helpmebackend.usecase.port.in.oauth2.OAuth2PortIn;
import seungyong.helpmebackend.usecase.port.out.cipher.CipherPortOut;
import seungyong.helpmebackend.usecase.port.out.github.GithubPortOut;
import seungyong.helpmebackend.usecase.port.out.jwt.JWTPortOut;
import seungyong.helpmebackend.usecase.port.out.redis.RedisPortOut;
import seungyong.helpmebackend.usecase.port.out.user.UserPortOut;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class OAuth2Service implements OAuth2PortIn {
    private final GithubPortOut githubPortOut;
    private final RedisPortOut redisPortOut;
    private final CipherPortOut cipherPortOut;
    private final JWTPortOut jwtPortOut;
    private final UserPortOut userPortOut;

    @Override
    public String generateLoginUrl() {
        // 랜덤 문자열 생성
        byte[] randomBytes = new byte[32];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(randomBytes);

        String state = null, key = null;

        do {
            state = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
            key = RedisKey.OAUTH2_STATE_KEY.getValue() + state;
        } while (redisPortOut.exists(key));

        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(10);

        redisPortOut.save(key, "valid", expireTime);

        return githubPortOut.generateLoginUrl(state);
    }

    @Override
    @Transactional
    public JWT signupOrLogin(String code, String state) {
        String stateKey = RedisKey.OAUTH2_STATE_KEY.getValue() + state;
        if (!redisPortOut.exists(stateKey)) { throw new CustomException(GlobalErrorCode.INVALID_OAUTH2_STATE); }
        redisPortOut.delete(stateKey);

        String accessToken = githubPortOut.getAccessToken(code);
        GithubUser githubUser = githubPortOut.getGithubUser(accessToken);
        String encryptedAccessToken = cipherPortOut.encrypt(accessToken);
        githubUser.setGithubToken(encryptedAccessToken);

        User user = userPortOut.getByGithubId(githubUser.getGithubId())
                .orElseGet(() -> userPortOut.save(UserPortOutMapper.INSTANCE.toDomainEntity(githubUser)));

        JWT jwt = jwtPortOut.generate(user.getId());

        String key = RedisKey.REFRESH_KEY.getValue() + user.getId();
        redisPortOut.save(key, jwt.getRefreshToken(), jwt.getRefreshTokenExpireTime());

        return jwt;
    }
}
