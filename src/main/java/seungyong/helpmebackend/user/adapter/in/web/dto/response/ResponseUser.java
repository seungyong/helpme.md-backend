package seungyong.helpmebackend.user.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import seungyong.helpmebackend.user.domain.entity.GithubUser;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.user.domain.entity.UserPlan;

import java.time.OffsetDateTime;

@Schema(description = "사용자 계정, 플랜, GitHub 연결 상태")
public record ResponseUser(
        Long id,
        String name,
        String avatarUrl,
        Long githubId,
        Plan plan,
        String status,
        String githubTokenStatus,
        OffsetDateTime githubTokenVerifiedAt,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt
) {
    private static final String GITHUB_AVATAR_URL = "https://avatars.githubusercontent.com/u/%d?v=4";

    public static ResponseUser from(User user) {
        GithubUser githubUser = user.getGithubUser();
        UserPlan userPlan = user.getPlan();

        return new ResponseUser(
                user.getId(),
                githubUser.getName(),
                GITHUB_AVATAR_URL.formatted(githubUser.getGithubId()),
                githubUser.getGithubId(),
                new Plan(
                        userPlan.code().getDatabaseValue(),
                        userPlan.projectLimit(),
                        userPlan.expiresAt()
                ),
                user.getStatus().getDatabaseValue(),
                githubUser.getTokenStatus().getDatabaseValue(),
                githubUser.getTokenVerifiedAt(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }

    public record Plan(
            String code,
            short projectLimit,
            OffsetDateTime expiresAt
    ) {
    }
}
