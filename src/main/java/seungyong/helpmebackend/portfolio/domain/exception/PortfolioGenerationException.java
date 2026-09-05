package seungyong.helpmebackend.portfolio.domain.exception;

import lombok.Getter;

@Getter
public class PortfolioGenerationException extends RuntimeException {
    private final String errorCode;

    public PortfolioGenerationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
