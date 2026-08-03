package seungyong.helpmebackend.github.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import seungyong.helpmebackend.github.application.port.in.GithubAppPortIn;
import seungyong.helpmebackend.github.application.port.in.result.GithubInstallationsResult;
import seungyong.helpmebackend.github.application.port.in.result.GithubRepositoriesResult;
import seungyong.helpmebackend.github.application.port.out.GithubAppPortOut;
import seungyong.helpmebackend.github.domain.entity.GithubInstallation;
import seungyong.helpmebackend.github.domain.entity.GithubRepository;
import seungyong.helpmebackend.github.domain.entity.GithubRepositoryPage;
import seungyong.helpmebackend.github.domain.exception.GithubErrorCode;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.user.domain.exception.UserErrorCode;
import seungyong.helpmebackend.user.domain.type.GithubTokenStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class GithubAppService implements GithubAppPortIn {
    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAX_PAGE_SIZE = 100;

    private final GithubAppPortOut githubAppPortOut;
    private final UserPortOut userPortOut;
    private final CipherPortOut cipherPortOut;
    private final ProjectPortOut projectPortOut;

    @Override
    public GithubInstallationsResult getInstallations(Long userId) {
        User user = getActiveUser(userId);
        String accessToken = decryptToken(user);

        List<GithubInstallation> installations = verifyGithubToken(
                user,
                () -> githubAppPortOut.getInstallations(userId, accessToken)
        );
        return GithubInstallationsResult.from(installations);
    }

    @Override
    public GithubRepositoriesResult getRepositories(
            Long userId,
            Long installationId,
            String query,
            String cursor,
            Integer size
    ) {
        if (installationId == null || installationId < 1) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }

        int normalizedSize = normalizeSize(size);
        int page = parsePageCursor(cursor);
        String normalizedQuery = query == null ? "" : query.trim();
        User user = getActiveUser(userId);
        String accessToken = decryptToken(user);

        GithubRepositoryPage repositoryPage = verifyGithubToken(
                user,
                () -> githubAppPortOut.getRepositories(
                        userId,
                        accessToken,
                        installationId,
                        normalizedQuery,
                        page,
                        normalizedSize
                )
        );

        List<Long> githubRepoIds = repositoryPage.repositories().stream()
                .map(GithubRepository::githubRepoId)
                .toList();
        Set<Long> connectedRepoIds = projectPortOut.getConnectedGithubRepoIds(
                userId,
                githubRepoIds
        );
        List<GithubRepositoriesResult.Item> items = repositoryPage.repositories().stream()
                .map(repository -> new GithubRepositoriesResult.Item(
                        repository,
                        connectedRepoIds.contains(repository.githubRepoId())
                ))
                .toList();

        return new GithubRepositoriesResult(
                items,
                new GithubRepositoriesResult.Page(
                        repositoryPage.nextCursor(),
                        repositoryPage.hasNext()
                )
        );
    }

    private User getActiveUser(Long userId) {
        User user = userPortOut.getById(userId);
        if (!user.isAuthenticationAllowed()) {
            throw new CustomException(UserErrorCode.USER_DELETION_IN_PROGRESS);
        }
        return user;
    }

    private String decryptToken(User user) {
        return cipherPortOut.decrypt(user.getGithubUser().getGithubToken().value());
    }

    private <T> T verifyGithubToken(User user, Supplier<T> action) {
        OffsetDateTime verifiedAt = OffsetDateTime.now(ZoneOffset.UTC);
        try {
            T result = action.get();
            user.recordGithubTokenVerification(GithubTokenStatus.VALID, verifiedAt);
            userPortOut.save(user);
            return result;
        } catch (CustomException exception) {
            if (exception.getErrorCode() == GithubErrorCode.GITHUB_CONNECTION_REVOKED) {
                user.recordGithubTokenVerification(GithubTokenStatus.REVOKED, verifiedAt);
                userPortOut.save(user);
            }
            throw exception;
        }
    }

    private int normalizeSize(Integer size) {
        int normalized = size == null ? DEFAULT_PAGE_SIZE : size;
        if (normalized < 1 || normalized > MAX_PAGE_SIZE) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
        return normalized;
    }

    private int parsePageCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 1;
        }

        try {
            int page = Integer.parseInt(cursor);
            if (page < 1) {
                throw new NumberFormatException("non-positive page cursor");
            }
            return page;
        } catch (NumberFormatException exception) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }
}
