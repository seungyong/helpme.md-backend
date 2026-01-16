package seungyong.helpmebackend.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import seungyong.helpmebackend.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum RepositoryErrorCode implements ErrorCode {
    REPOSITORY_CANNOT_PULL(HttpStatus.FORBIDDEN, "레포지토리 정보를 가져올 권한이 없습니다.", "REPO_40301"),

    README_NOT_BASE64(HttpStatus.BAD_REQUEST, "README.md가 base64로 인코딩되어 있지 않습니다.", "REPO_50001")
    ;

    private final String name = this.name();
    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;
}
