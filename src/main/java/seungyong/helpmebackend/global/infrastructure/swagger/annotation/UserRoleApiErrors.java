package seungyong.helpmebackend.global.infrastructure.swagger.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * User Role API의 공통 인증 및 회원 탈퇴 상태 오류를 Swagger에 추가합니다.
 * 실제 인증과 사용자 상태 검증은 Security 설정과 AuthenticationFilter가 담당합니다.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UserRoleApiErrors {
}
