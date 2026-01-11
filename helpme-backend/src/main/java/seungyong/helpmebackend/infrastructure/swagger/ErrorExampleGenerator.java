package seungyong.helpmebackend.infrastructure.swagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.examples.Example;
import lombok.extern.slf4j.Slf4j;
import seungyong.helpmebackend.common.exception.ErrorCode;
import seungyong.helpmebackend.common.exception.ErrorResponse;
import seungyong.helpmebackend.infrastructure.mapper.CustomTimeStamp;

import java.util.HashMap;
import java.util.Map;

/**
 * Swagger API Error 응답 예시를 자동으로 생성하는 클래스입니다.
 * */
@Slf4j
public class ErrorExampleGenerator {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * {@link ErrorResponse}를 사용하여 Swagger Error 예시를 생성하는 메소드
     * */
    public static String generateErrorExample(ErrorCode errorCode) {
        try {
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(errorCode.getHttpStatus().value())
                    .errorCode(errorCode.getErrorCode())
                    .error(errorCode.getHttpStatus().name())
                    .code(errorCode.getName())
                    .message(errorCode.getMessage())
                    .build();

            // 들여쓰기된 JSON 생성
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(errorResponse);
        } catch (Exception e) {
            log.error("Error generating error example: {}", e.getMessage());
            return generateFallbackExample(errorCode);
        }
    }

    /**
     *  generateErrorExample 실패 시 직접 String을 생성하여 주입 <br />
     *  순수한 Pure Java 코드 로직
     * */
    private static String generateFallbackExample(ErrorCode errorCode) {
        return String.format("""
                {
                    "timestamp": "%s",
                    "status": %d,
                    "error": "%s",
                    "message": "%s",
                    "code": "%s",
                    "errorCode": "%s"
                }
                """,
                new CustomTimeStamp(),
                errorCode.getHttpStatus().value(),
                errorCode.getHttpStatus().name(),
                errorCode.getMessage(),
                errorCode.getName(),
                errorCode.getErrorCode()
        );
    }

    /**
     * Swagger Error를 직접 처리하는 메소드 <br />
     * Swagger의 {@link Example}을 사용하여 Error Example Response를 정의
     * */
    public static Map<String, Example> generateErrorExamples(
            Class<? extends Enum<? extends ErrorCode>> errorCodeClass,
            String[] errorCodeNames
    ) {
        Map<String, Example> examples = new HashMap<>();

        for (String errorCodeName : errorCodeNames) {
            try {
                ErrorCode errorCode = getErrorCodeByName(errorCodeClass, errorCodeName);

                if (errorCode == null) {
                    log.warn("ErrorCode not found for name: {}", errorCodeName);
                    continue;
                }

                Example example = new Example();
                example.summary(errorCode.getErrorCode());
                example.description(String.format("%s - %s", errorCode.getErrorCode(), errorCode.getMessage()));
                example.value(generateErrorExample(errorCode));

                examples.put(errorCodeName, example);
            } catch (Exception e) {
                log.error("Error generating example for {}: {}", errorCodeName, e.getMessage());
            }
        }

        return examples;
    }

    /**
     * 특정 ErrorCode Enum에서 특정 이름을 가진 {@link ErrorCode}를 반환
     * */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ErrorCode getErrorCodeByName(
            Class<? extends Enum<? extends ErrorCode>> errorCodeClass,
            String errorCodeName) {

        try {
            // Raw type으로 변환하여 Enum.valueOf 사용
            return (ErrorCode) Enum.valueOf((Class) errorCodeClass, errorCodeName);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid ErrorCode name: {} in class: {}", errorCodeName, errorCodeClass.getSimpleName());
            return null;
        } catch (Exception e) {
            log.error("Error getting ErrorCode: {} in class: {}", errorCodeName, errorCodeClass.getSimpleName(), e);
            return null;
        }
    }
}
