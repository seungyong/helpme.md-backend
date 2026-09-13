package seungyong.helpmebackend.portfolio.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import seungyong.helpmebackend.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum PortfolioExportErrorCode implements ErrorCode {
    EXPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "내보내기 요청을 찾을 수 없습니다.", "EXPORT_40401"),
    INVALID_EXPORT_STATE(HttpStatus.CONFLICT, "현재 내보내기 상태에서는 요청을 처리할 수 없습니다.", "EXPORT_40901"),
    PDF_EXPIRED(HttpStatus.GONE, "PDF 다운로드 기간이 만료되었습니다.", "EXPORT_41001"),
    PDF_RENDER_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PDF 생성에 실패했습니다.", "EXPORT_50001");

    private final String name = this.name();
    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;
}
