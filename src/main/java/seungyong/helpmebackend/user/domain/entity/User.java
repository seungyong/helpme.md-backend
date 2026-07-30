package seungyong.helpmebackend.user.domain.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;
import seungyong.helpmebackend.user.domain.type.UserStatus;

import java.time.OffsetDateTime;
import java.util.Objects;

@Getter
@ToString
public class User {
    private Long id;
    private GithubUser githubUser;
    private UserPlan plan;
    private UserStatus status;
    private OffsetDateTime lastLoginAt;
    private UserDeletion deletion;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public User(Long id, GithubUser githubUser) {
        this(id, githubUser, UserPlan.free(), UserStatus.ACTIVE, null, UserDeletion.none(), null, null);
    }

    public User(Long id, GithubUser githubUser, UserStatus status) {
        this(id, githubUser, UserPlan.free(), status, null, UserDeletion.none(), null, null);
    }

    @Builder
    private User(
            Long id,
            GithubUser githubUser,
            UserPlan plan,
            UserStatus status,
            OffsetDateTime lastLoginAt,
            UserDeletion deletion,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.id = id;
        this.githubUser = Objects.requireNonNull(githubUser, "GitHub 사용자 정보는 null일 수 없습니다.");
        this.plan = plan == null ? UserPlan.free() : plan;
        this.status = status == null ? UserStatus.ACTIVE : status;
        this.lastLoginAt = lastLoginAt;
        this.deletion = deletion == null ? UserDeletion.none() : deletion;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 새로운 사용자를 등록하는 정적 팩토리 메서드
     * @param githubUser GitHub 사용자 정보
     * @param authenticatedAt 인증 시각
     * @return 새로 생성된 User 객체
     */
    public static User register(GithubUser githubUser, OffsetDateTime authenticatedAt) {
        return User.builder()
                .githubUser(githubUser)
                .plan(UserPlan.free())
                .status(UserStatus.ACTIVE)
                .lastLoginAt(Objects.requireNonNull(authenticatedAt, "로그인 시각은 null일 수 없습니다."))
                .deletion(UserDeletion.none())
                .build();
    }

    /**
     * 사용자가 활성화된 상태인지 확인하는 메서드
     * @return true는 활성화 상태, false는 삭제중 또는 삭제 실패 상태
     */
    public boolean isAuthenticationAllowed() {
        return status.allowsAuthentication();
    }

    /**
     * 사용자가 성공적으로 로그인했음을 기록하는 메서드
     * @param authenticatedGithubUser 인증된 GitHub 사용자 정보
     * @param authenticatedAt 인증 시각
     */
    public void recordSuccessfulLogin(GithubUser authenticatedGithubUser, OffsetDateTime authenticatedAt) {
        if (!isAuthenticationAllowed()) {
            throw new IllegalStateException("활성 상태인 사용자만 로그인할 수 있습니다.");
        }
        if (!githubUser.getGithubId().equals(authenticatedGithubUser.getGithubId())) {
            throw new IllegalArgumentException("다른 GitHub 사용자의 인증 정보로 갱신할 수 없습니다.");
        }

        this.githubUser = Objects.requireNonNull(authenticatedGithubUser, "인증된 GitHub 사용자 정보는 null일 수 없습니다.");
        this.lastLoginAt = Objects.requireNonNull(authenticatedAt, "로그인 시각은 null일 수 없습니다.");
    }

    /**
     * 유저의 깃허브 토큰을 새로운 토큰으로 업데이트하는 메서드
     * <br /> <br />
     * - 반드시 EncryptedToken 객체(암호화 토큰)를 인자로 받아야 하며, 내부적으로 토큰 값을 추출하여 저장합니다.
     *
     * @param newToken 업데이트할 새로운 암호화된 토큰
     */
    public void updateGithubToken(EncryptedToken newToken) {
        this.githubUser.updateGithubToken(newToken);
    }
}
