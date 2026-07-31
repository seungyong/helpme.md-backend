package seungyong.helpmebackend.global.infrastructure.swagger;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.ApiErrorResponse;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.ApiErrorResponses;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.UserRoleApiErrors;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseOperationCustomizerTest {
    private final ErrorResponseOperationCustomizer customizer = new ErrorResponseOperationCustomizer();

    @Test
    @DisplayName("User Role API에는 탈퇴 처리 중 409 응답을 공통으로 문서화")
    void customize_adds_user_deletion_error_to_authenticated_api() throws Exception {
        Operation operation = new Operation().responses(new ApiResponses());

        customizer.customize(operation, handlerMethod("authenticatedApi"));

        assertThat(operation.getResponses()).containsKey("401").containsKey("409");
        assertThat(operation.getResponses().get("401").getContent()
                .get("application/json").getExamples())
                .containsKeys("NOT_FOUND_TOKEN", "EXPIRED_ACCESS_TOKEN", "INVALID_TOKEN");
        assertThat(operation.getResponses().get("409").getContent()
                .get("application/json").getExamples()
                .get("USER_DELETION_IN_PROGRESS").getValue().toString())
                .contains("USER_40901")
                .contains("sign_out");
    }

    @Test
    @DisplayName("인증이 필요하지 않은 API에는 탈퇴 처리 중 409 응답을 추가하지 않음")
    void customize_does_not_add_user_deletion_error_to_public_api() throws Exception {
        Operation operation = new Operation().responses(new ApiResponses());

        customizer.customize(operation, handlerMethod("publicApi"));

        assertThat(operation.getResponses()).containsKey("500").doesNotContainKey("409");
    }

    @Test
    @DisplayName("Controller에 선언한 User Role 공통 오류도 모든 메서드에 문서화")
    void customize_applies_class_level_user_role_errors() throws Exception {
        Operation operation = new Operation().responses(new ApiResponses());
        Method method = ClassLevelUserRoleController.class.getDeclaredMethod("api");

        customizer.customize(operation, new HandlerMethod(new ClassLevelUserRoleController(), method));

        assertThat(operation.getResponses()).containsKeys("401", "409");
    }

    private HandlerMethod handlerMethod(String methodName) throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod(methodName);
        return new HandlerMethod(new TestController(), method);
    }

    private static class TestController {
        @UserRoleApiErrors
        @ApiErrorResponses({
                @ApiErrorResponse(
                        responseCode = "401",
                        description = "인증 실패",
                        errorCodeClasses = GlobalErrorCode.class,
                        errorCodes = { "EXPIRED_ACCESS_TOKEN", "NOT_FOUND_TOKEN" }
                )
        })
        void authenticatedApi() {
        }

        @ApiErrorResponses({
                @ApiErrorResponse(
                        responseCode = "500",
                        description = "서버 오류",
                        errorCodeClasses = GlobalErrorCode.class,
                        errorCodes = { "INTERNAL_SERVER_ERROR" }
                )
        })
        void publicApi() {
        }
    }

    @UserRoleApiErrors
    private static class ClassLevelUserRoleController {
        void api() {
        }
    }
}
