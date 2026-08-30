package seungyong.helpmebackend.auth.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import seungyong.helpmebackend.auth.application.port.in.AuthPortIn;
import seungyong.helpmebackend.auth.application.port.out.OAuth2PortOut;
import seungyong.helpmebackend.auth.application.port.out.result.OAuthGithubUser;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.domain.type.RedisKey;
import seungyong.helpmebackend.global.application.port.out.JWTPortOut;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.user.domain.entity.JWTUser;
import seungyong.helpmebackend.user.domain.entity.User;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthPortIn {
    private final OAuth2PortOut oAuth2PortOut;
    private final RedisPortOut redisPortOut;
    private final JWTPortOut jwtPortOut;
    private final AuthenticatedUserWriter authenticatedUserWriter;

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
    public JWT signupOrLogin(String code, String state) {
        String stateKey = RedisKey.OAUTH2_STATE_KEY.getValue() + state;
        if (!redisPortOut.exists(stateKey)) {
            throw new CustomException(GlobalErrorCode.OAUTH_STATE_INVALID);
        }
        redisPortOut.delete(stateKey);

        String accessToken = oAuth2PortOut.getAccessToken(code).accessToken();
        OAuthGithubUser oauthInfo = oAuth2PortOut.getGithubUser(accessToken);

        OffsetDateTime authenticatedAt = OffsetDateTime.now(ZoneOffset.UTC);
        User user = authenticatedUserWriter.authenticate(oauthInfo, accessToken, authenticatedAt);

        JWT jwt = jwtPortOut.generate(new JWTUser(user.getId(), user.getGithubUser().getName()));

        String key = RedisKey.REFRESH_KEY.getValue() + jwt.getRefreshToken();
        Instant expireTime = jwt.getRefreshTokenExpireTime();
        redisPortOut.set(key, user.getId().toString(), expireTime);

        return jwt;
    }

}
