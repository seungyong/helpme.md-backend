package seungyong.helpmebackend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DocumentErrorCode implements ErrorCode {
    DOCUMENT_VERSION_CONFLICT(
            HttpStatus.CONFLICT,
            "다른 변경 사항이 먼저 저장되었습니다. 최신 내용을 다시 조회해 주세요.",
            "DOCUMENT_40901"
    );

    private final String name = this.name();
    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;
}
