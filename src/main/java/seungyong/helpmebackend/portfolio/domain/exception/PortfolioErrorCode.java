package seungyong.helpmebackend.portfolio.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import seungyong.helpmebackend.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum PortfolioErrorCode implements ErrorCode {
    PORTFOLIO_NOT_FOUND(HttpStatus.NOT_FOUND, "포트폴리오를 찾을 수 없습니다.", "PORTFOLIO_40401"),
    PORTFOLIO_SOURCE_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "저장된 회고를 하나 이상 선택해 주세요.", "PORTFOLIO_42201"),
    PORTFOLIO_PRIVATE_EVIDENCE_NOT_ALLOWED(HttpStatus.UNPROCESSABLE_ENTITY, "비공개 저장소 근거는 포트폴리오에 포함할 수 없습니다.", "PORTFOLIO_42202"),
    PORTFOLIO_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "포트폴리오 초안 생성에 실패했습니다.", "PORTFOLIO_50001"),
    PORTFOLIO_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "포트폴리오 생성 요청 한도를 초과했습니다.", "RATE_42902");

    private final String name = this.name();
    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;
}
