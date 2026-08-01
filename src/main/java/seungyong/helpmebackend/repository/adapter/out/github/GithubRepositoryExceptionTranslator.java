package seungyong.helpmebackend.repository.adapter.out.github;

import org.springframework.http.HttpStatus;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.ErrorCode;
import seungyong.helpmebackend.global.exception.GithubRateLimitException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.global.infrastructure.github.GithubApiException;
import seungyong.helpmebackend.global.infrastructure.github.GithubResponseParsingException;
import seungyong.helpmebackend.repository.domain.exception.RepositoryErrorCode;

import java.util.function.Supplier;

final class GithubRepositoryExceptionTranslator {
    private GithubRepositoryExceptionTranslator() {
    }

    /**
     * Github API 호출 시 발생할 수 있는 예외를 처리합니다. <br />
     * 단, 404 Not Found 발생 시 Fallback을 지정하지 않으면 상황에 맞는 {@link CustomException}을 발생시킵니다.
     * @param action {@link Supplier} - Github API 호출을 수행하는 Supplier
     * @return {@link T} - Github API 호출 결과
     */
    static <T> T execute(Supplier<T> action) {
        return execute(action, null);
    }

    /**
     * Github API 호출 시 발생할 수 있는 예외를 처리하고, 필요한 경우 지정된 Fallback을 실행합니다.
     * @param action {@link Supplier} - Github API 호출을 수행하는 Supplier
     * @param notFoundFallback {@link Supplier} - 404 Not Found 발생 시 실행할 Fallback Supplier (에러 발생, null 반환 등)
     * @return {@link T} - Github API 호출 결과 또는 Fallback 결과
     */
    static <T> T execute(Supplier<T> action, Supplier<T> notFoundFallback) {
        try {
            return action.get();
        } catch (GithubResponseParsingException e) {
            throw new CustomException(RepositoryErrorCode.JSON_PROCESSING_ERROR);
        } catch (GithubApiException e) {
            if (e.isRateLimited()) {
                throw new GithubRateLimitException(e.getRetryAfterSeconds());
            }
            if (e.hasStatus(HttpStatus.UNAUTHORIZED)) {
                throw new CustomException(RepositoryErrorCode.GITHUB_UNAUTHORIZED);
            }
            if (e.hasStatus(HttpStatus.FORBIDDEN)) {
                throw new CustomException(RepositoryErrorCode.GITHUB_FORBIDDEN);
            }
            if (e.hasStatus(HttpStatus.NOT_FOUND) && notFoundFallback != null) {
                return notFoundFallback.get();
            }

            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }

    /**
     * 404 Not Found 에러 발생 시, 지정된 ErrorCode로 CustomException을 발생시키는 Supplier를 반환합니다.
     * @param errorCode {@link ErrorCode} - 발생시킬 CustomException의 ErrorCode
     * @return {@link Supplier} - 404 Not Found 발생 시 CustomException을 던지는 Supplier
     */
    static <T> Supplier<T> failWith(ErrorCode errorCode) {
        return () -> {
            throw new CustomException(errorCode);
        };
    }

    static void run(Runnable action) {
        execute(() -> {
            action.run();
            return null;
        });
    }
}
