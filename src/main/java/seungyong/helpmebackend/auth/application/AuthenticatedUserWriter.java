package seungyong.helpmebackend.auth.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.auth.application.port.out.result.OAuthGithubUser;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.GithubUser;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.user.domain.exception.UserErrorCode;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
class AuthenticatedUserWriter {
    private final UserPortOut userPortOut;
    private final CipherPortOut cipherPortOut;

    @Transactional
    public User authenticate(
            OAuthGithubUser oauthInfo,
            String accessToken,
            OffsetDateTime authenticatedAt
    ) {
        User user = userPortOut.getByGithubId(oauthInfo.githubId())
                .map(existing -> updateExistingUser(
                        existing,
                        oauthInfo,
                        accessToken,
                        authenticatedAt
                ))
                .orElseGet(() -> User.register(
                        authenticatedGithubUser(oauthInfo, accessToken, authenticatedAt),
                        authenticatedAt
                ));

        return userPortOut.save(user);
    }

    private User updateExistingUser(
            User user,
            OAuthGithubUser oauthInfo,
            String accessToken,
            OffsetDateTime authenticatedAt
    ) {
        if (!user.isAuthenticationAllowed()) {
            throw new CustomException(UserErrorCode.USER_DELETION_IN_PROGRESS);
        }
        user.recordSuccessfulLogin(
                authenticatedGithubUser(oauthInfo, accessToken, authenticatedAt),
                authenticatedAt
        );
        return user;
    }

    private GithubUser authenticatedGithubUser(
            OAuthGithubUser oauthInfo,
            String accessToken,
            OffsetDateTime authenticatedAt
    ) {
        return GithubUser.authenticated(
                oauthInfo.name(),
                oauthInfo.githubId(),
                new EncryptedToken(cipherPortOut.encrypt(accessToken)),
                authenticatedAt
        );
    }
}
