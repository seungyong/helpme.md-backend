package seungyong.helpmebackend.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import seungyong.helpmebackend.common.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum SectionErrorCode implements ErrorCode {
    NOT_FOUND_SECTIONS(HttpStatus.NOT_FOUND, "섹션을 찾을 수 없습니다.", "SECTION_40401"),
    ;

    private final String name = this.name();
    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;
}
