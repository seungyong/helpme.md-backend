package seungyong.helpmebackend.usecase.service.oauth2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.adapter.out.persistence.mapper.UserPortOutMapper;
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

@Service
@RequiredArgsConstructor
public class OAuth2Service implements OAuth2PortIn {
    private final GithubPortOut githubPortOut;
    private final RedisPortOut redisPortOut;
    private final CipherPortOut cipherPortOut;
    private final JWTPortOut jwtPortOut;
    private final UserPortOut userPortOut;

    @Override
    @Transactional
    public JWT signupOrLogin(String code) {
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
