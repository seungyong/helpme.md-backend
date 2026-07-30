package seungyong.helpmebackend.auth.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.auth.adapter.in.web.dto.response.ResponseInstallations;
import seungyong.helpmebackend.auth.application.port.in.AuthPortIn;
import seungyong.helpmebackend.auth.application.port.out.OAuth2PortOut;
import seungyong.helpmebackend.auth.application.port.out.result.OAuthGithubUser;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.global.domain.type.RedisKey;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.global.application.port.out.JWTPortOut;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.GithubUser;
import seungyong.helpmebackend.user.domain.entity.JWTUser;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.user.domain.exception.UserErrorCode;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthPortIn {
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
        OAuthGithubUser oauthInfo = oAuth2PortOut.getGithubUser(accessToken);

        Optional<User> existingUser = userPortOut.getByGithubId(oauthInfo.githubId());
        if (existingUser.isPresent() && !existingUser.get().isAuthenticationAllowed()) {
            throw new CustomException(UserErrorCode.USER_DELETION_IN_PROGRESS);
        }

        OffsetDateTime authenticatedAt = OffsetDateTime.now(ZoneOffset.UTC);
        String encryptedAccessToken = cipherPortOut.encrypt(accessToken);
        GithubUser authenticatedGithubUser = GithubUser.authenticated(
                oauthInfo.name(),
                oauthInfo.githubId(),
                new EncryptedToken(encryptedAccessToken),
                authenticatedAt
        );

        User user = existingUser
                .map(existing -> {
                    existing.recordSuccessfulLogin(authenticatedGithubUser, authenticatedAt);
                    return userPortOut.save(existing);
                })
                .orElseGet(() -> userPortOut.save(User.register(authenticatedGithubUser, authenticatedAt)));

        JWT jwt = jwtPortOut.generate(new JWTUser(user.getId(), user.getGithubUser().getName()));

        String key = RedisKey.REFRESH_KEY.getValue() + jwt.getRefreshToken();
        Instant expireTime = jwt.getRefreshTokenExpireTime();
        redisPortOut.set(key, user.getId().toString(), expireTime);

        return jwt;
    }

    @Override
    public ResponseInstallations getInstallations(Long userId) {
        User user = userPortOut.getById(userId);
        String decryptedToken = cipherPortOut.decrypt(user.getGithubUser().getGithubToken().value());
        return new ResponseInstallations(oAuth2PortOut.getInstallations(decryptedToken));
    }
}
