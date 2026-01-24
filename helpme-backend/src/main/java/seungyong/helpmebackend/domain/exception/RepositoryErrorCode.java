package seungyong.helpmebackend.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import seungyong.helpmebackend.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum RepositoryErrorCode implements ErrorCode {
    BAD_REQUEST_SAME_BRANCH(HttpStatus.BAD_REQUEST, "같은 브랜치에 Pull Request를 요청할 수 없습니다.", "REPO_40001"),

    GITHUB_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "GitHub 연동이 해제되었습니다.\n다시 로그인해주세요.", "REPO_40101"),

    GITHUB_FORBIDDEN(HttpStatus.FORBIDDEN, "GitHub App 권한이 부족합니다.\n앱 설정을 다시 확인해주세요.", "REPO_40301"),

    REPOSITORY_README_NOT_FOUND(HttpStatus.NOT_FOUND, "레포지토리의 README.md를 찾을 수 없습니다.", "REPO_40401"),
    BRANCH_NOT_FOUND(HttpStatus.NOT_FOUND, "레포지토리의 브랜치를 찾을 수 없습니다.", "REPO_40402"),

    REPOSITORY_CANNOT_PULL(HttpStatus.FORBIDDEN, "레포지토리 정보를 가져올 권한이 없습니다.", "REPO_40301"),

    GITHUB_RATE_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "GitHub API 요청 한도를 초과했습니다.", "REPO_42901"),
    GITHUB_BRANCHES_TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "GitHub 브랜치 정보 요청이 너무 많습니다.", "REPO_42902"),

    README_NOT_BASE64(HttpStatus.BAD_REQUEST, "README.md가 base64로 인코딩되어 있지 않습니다.", "REPO_50001"),
    JSON_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "JSON 처리 중 오류가 발생했습니다.", "REPO_50002"),
    ;

    private final String name = this.name();
    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;
}
