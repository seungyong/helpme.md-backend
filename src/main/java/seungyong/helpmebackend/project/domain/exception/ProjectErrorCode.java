package seungyong.helpmebackend.project.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import seungyong.helpmebackend.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum ProjectErrorCode implements ErrorCode {
    PROJECT_ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "프로젝트 접근 권한이 없습니다.",
            "PROJECT_40301"
    ),
    PROJECT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "프로젝트를 찾을 수 없습니다.",
            "PROJECT_40401"
    ),
    PROJECT_NOT_ACTIVE(
            HttpStatus.CONFLICT,
            "활성 상태가 아닌 프로젝트입니다.",
            "PROJECT_40903"
    ),
    PROJECT_ALREADY_CONNECTED(
            HttpStatus.CONFLICT,
            "이미 연결된 GitHub Repository입니다.",
            "PROJECT_40901"
    ),
    PROJECT_LIMIT_EXCEEDED(
            HttpStatus.CONFLICT,
            "현재 플랜의 프로젝트 생성 한도에 도달했습니다.",
            "PROJECT_40902"
    ),
    PROJECT_SYNC_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "프로젝트 최초 동기화에 실패했습니다.",
            "PROJECT_50001"
    );

    private final String name = this.name();
    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;
}
