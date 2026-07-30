package seungyong.helpmebackend.user.adapter.out.persistence.mapper;

import seungyong.helpmebackend.user.adapter.out.persistence.entity.UserJpaEntity;
import seungyong.helpmebackend.user.domain.entity.GithubUser;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.user.domain.entity.UserDeletion;
import seungyong.helpmebackend.user.domain.entity.UserPlan;

public final class UserPersistenceMapper {
    public static final UserPersistenceMapper INSTANCE = new UserPersistenceMapper();

    private UserPersistenceMapper() {
    }

    public UserJpaEntity toJpaEntity(User user) {
        GithubUser githubUser = user.getGithubUser();
        UserPlan plan = user.getPlan();
        UserDeletion deletion = user.getDeletion();

        return UserJpaEntity.builder()
                .id(user.getId())
                .name(githubUser.getName())
                .githubId(githubUser.getGithubId())
                .githubToken(githubUser.getGithubToken())
                .planCode(plan.code())
                .projectLimit(plan.projectLimit())
                .planExpiresAt(plan.expiresAt())
                .status(user.getStatus())
                .githubTokenStatus(githubUser.getTokenStatus())
                .githubTokenVerifiedAt(githubUser.getTokenVerifiedAt())
                .lastLoginAt(user.getLastLoginAt())
                .deletionRequestedAt(deletion.requestedAt())
                .deletionErrorCode(deletion.errorCode())
                .deletionErrorMessage(deletion.errorMessage())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public User toDomainEntity(UserJpaEntity entity) {
        GithubUser githubUser = GithubUser.builder()
                .name(entity.getName())
                .githubId(entity.getGithubId())
                .githubToken(entity.getGithubToken())
                .tokenStatus(entity.getGithubTokenStatus())
                .tokenVerifiedAt(entity.getGithubTokenVerifiedAt())
                .build();

        return User.builder()
                .id(entity.getId())
                .githubUser(githubUser)
                .plan(new UserPlan(
                        entity.getPlanCode(),
                        entity.getProjectLimit(),
                        entity.getPlanExpiresAt()
                ))
                .status(entity.getStatus())
                .lastLoginAt(entity.getLastLoginAt())
                .deletion(new UserDeletion(
                        entity.getDeletionRequestedAt(),
                        entity.getDeletionErrorCode(),
                        entity.getDeletionErrorMessage()
                ))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
