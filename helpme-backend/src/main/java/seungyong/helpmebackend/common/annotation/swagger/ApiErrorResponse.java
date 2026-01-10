package seungyong.helpmebackend.common.annotation.swagger;

import seungyong.helpmebackend.common.exception.ErrorCode;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 단일 API 오류 응답을 정의하는 어노테이션입니다.
 * 항상 {@link ApiErrorResponses} 어노테이션과 함께 사용해야 합니다.
 * */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiErrorResponse {
    String responseCode();
    String description();
    Class<? extends Enum<? extends ErrorCode>> errorCodeClass();
    String[] errorCodes();
}
