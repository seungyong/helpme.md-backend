package seungyong.helpmebackend.common.exception;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;
import seungyong.helpmebackend.domain.mapper.CustomTimeStamp;

@Getter
@Builder
public class ErrorResponse {
    private final String timestamp = new CustomTimeStamp().toString();
    private final int status;
    private final String error;
    private final String message;
    private final String code;
    private final String errorCode;

    public static ResponseEntity<ErrorResponse> toResponseEntity(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.builder()
                        .status(errorCode.getHttpStatus().value())
                        .errorCode(errorCode.getErrorCode())
                        .error(errorCode.getHttpStatus().name())
                        .code(errorCode.getName())
                        .message(errorCode.getMessage())
                        .build()
                );
    }

    public static String toJson(ErrorCode errorCode) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(
                ErrorResponse.toResponseEntity(errorCode).getBody()
        );
    }
}
