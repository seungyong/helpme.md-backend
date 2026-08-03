package seungyong.helpmebackend.github.adapter.out.github;

import org.springframework.http.HttpStatus;
import seungyong.helpmebackend.github.domain.exception.GithubErrorCode;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GithubRateLimitException;
import seungyong.helpmebackend.global.infrastructure.github.GithubApiException;
import seungyong.helpmebackend.global.infrastructure.github.GithubResponseParsingException;

import java.util.function.Supplier;

final class GithubAppExceptionTranslator {
    private GithubAppExceptionTranslator() {
    }

    static <T> T installations(Supplier<T> action) {
        return execute(action, GithubErrorCode.GITHUB_CONNECTION_REVOKED, false);
    }

    static <T> T repositories(Supplier<T> action) {
        return execute(action, GithubErrorCode.GITHUB_PERMISSION_DENIED, true);
    }

    private static <T> T execute(
            Supplier<T> action,
            GithubErrorCode forbiddenError,
            boolean mapNotFound
    ) {
        try {
            return action.get();
        } catch (GithubResponseParsingException exception) {
            throw new CustomException(GithubErrorCode.GITHUB_UPSTREAM_ERROR);
        } catch (GithubApiException exception) {
            if (exception.isRateLimited()) {
                throw new GithubRateLimitException(
                        GithubErrorCode.GITHUB_RATE_LIMIT_EXCEEDED,
                        exception.getRetryAfterSeconds()
                );
            }
            if (exception.hasStatus(HttpStatus.UNAUTHORIZED)) {
                throw new CustomException(GithubErrorCode.GITHUB_CONNECTION_REVOKED);
            }
            if (exception.hasStatus(HttpStatus.FORBIDDEN)) {
                throw new CustomException(forbiddenError);
            }
            if (mapNotFound && exception.hasStatus(HttpStatus.NOT_FOUND)) {
                throw new CustomException(GithubErrorCode.GITHUB_RESOURCE_NOT_FOUND);
            }
            throw new CustomException(GithubErrorCode.GITHUB_UPSTREAM_ERROR);
        }
    }
}
