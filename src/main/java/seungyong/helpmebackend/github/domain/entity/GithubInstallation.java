package seungyong.helpmebackend.github.domain.entity;

import seungyong.helpmebackend.github.domain.type.GithubAccountType;
import seungyong.helpmebackend.github.domain.type.GithubRepositorySelection;

public record GithubInstallation(
        // GitHub App 설치 ID
        long installationId,
        // Repository Github 계정 (User: 사용자, Organization: 조직명)
        String login,
        // GitHub 계정 유형 (User, Organization)
        GithubAccountType type,
        // GitHub Repository 선택 범위 (all, selected)
        GithubRepositorySelection repositorySelection,
        // 해당 Installation 안 Repository 수
        int repositoryCount
) {
}
