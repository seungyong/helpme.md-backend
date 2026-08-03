package seungyong.helpmebackend.github.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import seungyong.helpmebackend.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum GithubErrorCode implements ErrorCode {
    GITHUB_CONNECTION_REVOKED(
            HttpStatus.FORBIDDEN,
            "GitHub 연결이 회수되었습니다. 다시 연결해주세요.",
            "GITHUB_40301"
    ),
    GITHUB_PERMISSION_DENIED(
            HttpStatus.FORBIDDEN,
            "GitHub App의 Repository 접근 권한이 부족합니다.",
            "GITHUB_40302"
    ),
    GITHUB_RESOURCE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "GitHub installation 또는 Repository를 찾을 수 없습니다.",
            "GITHUB_40401"
    ),
    GITHUB_RATE_LIMIT_EXCEEDED(
            HttpStatus.TOO_MANY_REQUESTS,
            "GitHub API 요청 한도를 초과했습니다.",
            "RATE_42901"
    ),
    GITHUB_UPSTREAM_ERROR(
            HttpStatus.BAD_GATEWAY,
            "GitHub API가 비정상 응답을 반환했습니다.",
            "GITHUB_50201"
    );

    private final String name = this.name();
    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;
}
