package seungyong.helpmebackend.user.domain.entity;

import lombok.Builder;
import lombok.Getter;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;
import seungyong.helpmebackend.user.domain.type.GithubTokenStatus;

import java.time.OffsetDateTime;
import java.util.Objects;

@Getter
public class GithubUser {
    private String name;
    private final Long githubId;
    private EncryptedToken githubToken;
    private GithubTokenStatus tokenStatus;
    private OffsetDateTime tokenVerifiedAt;

    public GithubUser(String name, Long githubId, EncryptedToken githubToken) {
        this(name, githubId, githubToken, GithubTokenStatus.UNKNOWN, null);
    }

    @Builder
    private GithubUser(
            String name,
            Long githubId,
            EncryptedToken githubToken,
            GithubTokenStatus tokenStatus,
            OffsetDateTime tokenVerifiedAt
    ) {
        this.name = Objects.requireNonNull(name, "GitHub 사용자 이름은 null일 수 없습니다.");
        this.githubId = Objects.requireNonNull(githubId, "GitHub 사용자 ID는 null일 수 없습니다.");
        this.githubToken = Objects.requireNonNull(githubToken, "GitHub 토큰은 null일 수 없습니다.");
        this.tokenStatus = tokenStatus == null ? GithubTokenStatus.UNKNOWN : tokenStatus;
        this.tokenVerifiedAt = tokenVerifiedAt;
    }

    /**
     * GitHub 인증이 완료된 사용자를 생성하는 정적 팩토리 메서드입니다.
     * @param name Github 사용자 이름
     * @param githubId GitHub 사용자 ID
     * @param githubToken 암호화된 GitHub 토큰
     * @param authenticatedAt 인증 시각
     * @return 인증이 완료된 GithubUser 객체
     */
    public static GithubUser authenticated(
            String name,
            Long githubId,
            EncryptedToken githubToken,
            OffsetDateTime authenticatedAt
    ) {
        return GithubUser.builder()
                .name(name)
                .githubId(githubId)
                .githubToken(githubToken)
                .tokenStatus(GithubTokenStatus.VALID)
                .tokenVerifiedAt(Objects.requireNonNull(authenticatedAt, "GitHub 인증 시각은 null일 수 없습니다."))
                .build();
    }

    /**
     * 깃허브 토큰을 업데이트하는 메서드입니다.
     * <br /> <br />
     * - 반드시 EncryptedToken 객체(암호화 토큰)를 인자로 받아야 하며, 내부적으로 토큰 값을 추출하여 저장합니다.
     *
     * @param newToken 업데이트할 새로운 암호화된 토큰
     */
    public void updateGithubToken(EncryptedToken newToken) {
        if (newToken == null) {
            throw new IllegalArgumentException("새로운 토큰은 null일 수 없습니다.");
        }

        this.githubToken = newToken;
    }

    public void recordTokenVerification(
            GithubTokenStatus verifiedStatus,
            OffsetDateTime verifiedAt
    ) {
        if (verifiedStatus == null || verifiedStatus == GithubTokenStatus.UNKNOWN) {
            throw new IllegalArgumentException("검증 결과는 valid 또는 revoked여야 합니다.");
        }

        this.tokenStatus = verifiedStatus;
        this.tokenVerifiedAt = Objects.requireNonNull(
                verifiedAt,
                "GitHub 토큰 검증 시각은 null일 수 없습니다."
        );
    }
}
