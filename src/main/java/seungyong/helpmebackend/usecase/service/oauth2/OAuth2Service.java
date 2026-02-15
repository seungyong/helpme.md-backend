package seungyong.helpmebackend.usecase.service.oauth2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.adapter.in.web.dto.installation.response.ResponseInstallations;
import seungyong.helpmebackend.adapter.out.persistence.mapper.UserPortOutMapper;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.domain.entity.user.GithubUser;
import seungyong.helpmebackend.domain.entity.user.JWTUser;
import seungyong.helpmebackend.domain.entity.user.User;
import seungyong.helpmebackend.infrastructure.jwt.JWT;
import seungyong.helpmebackend.infrastructure.redis.RedisKey;
import seungyong.helpmebackend.usecase.port.in.oauth2.OAuth2PortIn;
import seungyong.helpmebackend.usecase.port.out.cipher.CipherPortOut;
import seungyong.helpmebackend.usecase.port.out.github.oauth2.OAuth2PortOut;
import seungyong.helpmebackend.usecase.port.out.jwt.JWTPortOut;
import seungyong.helpmebackend.usecase.port.out.redis.RedisPortOut;
import seungyong.helpmebackend.usecase.port.out.user.UserPortOut;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class OAuth2Service implements OAuth2PortIn {
    private final OAuth2PortOut oAuth2PortOut;
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

        Instant expireTime = Instant.now().plus(10, ChronoUnit.MINUTES);
        redisPortOut.set(key, "valid", expireTime);

        return oAuth2PortOut.generateLoginUrl(state);
    }

    @Override
    @Transactional
    public JWT signupOrLogin(String code, String state) {
        String stateKey = RedisKey.OAUTH2_STATE_KEY.getValue() + state;
        if (!redisPortOut.exists(stateKey)) { throw new CustomException(GlobalErrorCode.INVALID_OAUTH2_STATE); }
        redisPortOut.delete(stateKey);

        String accessToken = oAuth2PortOut.getAccessToken(code).accessToken();
        GithubUser githubUser = oAuth2PortOut.getGithubUser(accessToken);
        String encryptedAccessToken = cipherPortOut.encrypt(accessToken);
        githubUser.setGithubToken(encryptedAccessToken);

        User user = userPortOut.getByGithubId(githubUser.getGithubId())
                .orElseGet(() -> userPortOut.save(UserPortOutMapper.INSTANCE.toDomainEntity(githubUser)));

        if (user.isDiffToken(encryptedAccessToken)) {
            user.updateGithubToken(encryptedAccessToken);
            userPortOut.save(user);
        }

        JWT jwt = jwtPortOut.generate(new JWTUser(user.getId(), user.getGithubUser().getName()));

        String key = RedisKey.REFRESH_KEY.getValue() + user.getId();
        Instant expireTime = jwt.getRefreshTokenExpireTime();
        redisPortOut.set(key, jwt.getRefreshToken(), expireTime);

        return jwt;
    }

    @Override
    public ResponseInstallations getInstallations(Long userId) {
        User user = userPortOut.getById(userId);
        String decryptedToken = cipherPortOut.decrypt(user.getGithubUser().getGithubToken());
        return new ResponseInstallations(oAuth2PortOut.getInstallations(decryptedToken));
    }
}
