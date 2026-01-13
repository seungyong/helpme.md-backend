package seungyong.helpmebackend.infrastructure.swagger.annotation;

import java.lang.annotation.*;

/**
 * 여러 개의 API 오류 응답을 묶어놓는 어노테이션입니다.
 * */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiErrorResponses {
    ApiErrorResponse[] value();
}
