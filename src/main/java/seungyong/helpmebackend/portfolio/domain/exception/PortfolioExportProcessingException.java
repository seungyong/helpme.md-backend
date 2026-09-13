package seungyong.helpmebackend.portfolio.domain.exception;

import lombok.Getter;

@Getter
public class PortfolioExportProcessingException extends RuntimeException {
    private final String errorCode;

    public PortfolioExportProcessingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
