package seungyong.helpmebackend.section.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import seungyong.helpmebackend.github.application.port.in.GithubRepositoryAccessPortIn;
import seungyong.helpmebackend.github.domain.entity.GithubRepository;
import seungyong.helpmebackend.github.domain.exception.GithubErrorCode;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.global.domain.type.RedisKey;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.exception.ProjectErrorCode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReadmeComponentRepositoryAccessResolver {
    private static final long PERMISSION_CACHE_MINUTES = 5;

    private final ProjectPortOut projectPortOut;
    private final GithubRepositoryAccessPortIn githubRepositoryAccessPortIn;
    private final RedisPortOut redisPortOut;

    public Project resolveWritable(Long userId, String owner, String name) {
        validateRequest(userId, owner, name);
        String repoFullName = owner + "/" + name;
        Project project = projectPortOut.getByUserIdAndRepoFullName(userId, repoFullName)
                .orElseThrow(() -> new CustomException(
                        GithubErrorCode.GITHUB_RESOURCE_NOT_FOUND
                ));
        if (!project.isActive()) {
            throw new CustomException(ProjectErrorCode.PROJECT_NOT_ACTIVE);
        }

        String cacheKey = RedisKey.GITHUB_COMPONENT_AUTH_KEY.getValue()
                + userId + ":" + repoFullName;
        if (hasCachedPermission(cacheKey)) {
            return project;
        }

        validateGithubRepository(userId, project, repoFullName);
        cachePermission(cacheKey);
        return project;
    }

    private boolean hasCachedPermission(String cacheKey) {
        try {
            return redisPortOut.exists(cacheKey);
        } catch (CustomException exception) {
            if (exception.getErrorCode() != GlobalErrorCode.REDIS_ERROR) {
                throw exception;
            }
            log.warn("README component GitHub 권한 캐시 조회에 실패해 권한을 다시 검증합니다.");
            return false;
        }
    }

    private void cachePermission(String cacheKey) {
        try {
            redisPortOut.set(
                    cacheKey,
                    "writable",
                    Instant.now().plus(PERMISSION_CACHE_MINUTES, ChronoUnit.MINUTES)
            );
        } catch (CustomException exception) {
            if (exception.getErrorCode() != GlobalErrorCode.REDIS_ERROR) {
                throw exception;
            }
            log.warn("README component GitHub 권한 캐시 저장에 실패했습니다.");
        }
    }

    private void validateGithubRepository(
            Long userId,
            Project project,
            String repoFullName
    ) {
        if (project.getGithubInstallationId() == null
                || project.getGithubRepoId() == null) {
            throw new CustomException(GithubErrorCode.GITHUB_RESOURCE_NOT_FOUND);
        }
        GithubRepository repository = githubRepositoryAccessPortIn.getRepository(
                userId,
                project.getGithubInstallationId(),
                project.getGithubRepoId()
        );
        if (!repository.fullName().equalsIgnoreCase(repoFullName)) {
            throw new CustomException(GithubErrorCode.GITHUB_RESOURCE_NOT_FOUND);
        }
        if (!repository.permissions().push()) {
            throw new CustomException(GithubErrorCode.GITHUB_PERMISSION_DENIED);
        }
    }

    private void validateRequest(Long userId, String owner, String name) {
        if (userId == null || !StringUtils.hasText(owner) || !StringUtils.hasText(name)) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }
}
