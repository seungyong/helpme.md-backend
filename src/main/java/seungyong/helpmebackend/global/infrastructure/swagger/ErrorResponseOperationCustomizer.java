package seungyong.helpmebackend.global.infrastructure.swagger;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import seungyong.helpmebackend.global.exception.ErrorCode;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.ApiErrorResponse;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.ApiErrorResponses;
import seungyong.helpmebackend.global.infrastructure.swagger.annotation.UserRoleApiErrors;
import seungyong.helpmebackend.user.domain.exception.UserErrorCode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Operation에 ApiErrorResponse 어노테이션 기반 에러 응답 추가 <br />
 * OperationCustomizer는 각 Endpoint마다 호출됨
 */
@Slf4j
@Component
public class ErrorResponseOperationCustomizer implements OperationCustomizer {
    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        processApiErrorResponses(operation, handlerMethod);
        return operation;
    }

    private void processApiErrorResponses(Operation operation, HandlerMethod handlerMethod) {
        ApiErrorResponses annotation = handlerMethod.getMethodAnnotation(ApiErrorResponses.class);

        if (hasUserRoleApiErrors(handlerMethod)) {
            addUserRoleApiErrors(operation);
        }

        if (annotation != null) {
            for (ApiErrorResponse errorResponse : annotation.value()) {
                addErrorResponseToOperation(operation, errorResponse);
            }
        }
    }

    private boolean hasUserRoleApiErrors(HandlerMethod handlerMethod) {
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), UserRoleApiErrors.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), UserRoleApiErrors.class);
    }

    private void addUserRoleApiErrors(Operation operation) {
        addErrorResponseToOperation(
                operation,
                "401",
                "인증에 실패했습니다.",
                List.of(GlobalErrorCode.class),
                new String[]{ "NOT_FOUND_TOKEN", "EXPIRED_ACCESS_TOKEN", "INVALID_TOKEN" }
        );
        addUserDeletionError(operation);
    }

    private void addUserDeletionError(Operation operation) {
        addErrorResponseToOperation(
                operation,
                "409",
                "회원 탈퇴 처리 중인 사용자입니다.",
                List.of(UserErrorCode.class),
                new String[]{ "USER_DELETION_IN_PROGRESS" }
        );
    }

    /**
     * Operation에 에러 응답 추가
     */
    private void addErrorResponseToOperation(Operation operation, ApiErrorResponse errorResponse) {
        addErrorResponseToOperation(
                operation,
                errorResponse.responseCode(),
                errorResponse.description(),
                Arrays.asList(errorResponse.errorCodeClasses()),
                errorResponse.errorCodes()
        );
    }

    private void addErrorResponseToOperation(
            Operation operation,
            String responseCode,
            String description,
            Iterable<Class<? extends Enum<? extends ErrorCode>>> errorCodeClasses,
            String[] errorCodes
    ) {
        try {
            Map<String, Example> examples = new HashMap<>();

            for (Class<? extends Enum<? extends ErrorCode>> errorCodeClass : errorCodeClasses) {
                Map<String, Example> error = ErrorExampleGenerator.generateErrorExamples(
                        errorCodeClass,
                        errorCodes
                );

                examples.putAll(error);
            }

            if (examples.isEmpty()) {
                return;
            }

            ApiResponse existingResponse = operation.getResponses().get(responseCode);
            if (mergeExamples(existingResponse, examples)) {
                return;
            }

            MediaType mediaType = new MediaType()
                    // ErrorResponse 스키마 참조
                    .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                    .examples(examples);

            Content content = new Content()
                    .addMediaType("application/json", mediaType);

            ApiResponse apiResponse = new ApiResponse()
                    .description(description)
                    .content(content);

            operation.getResponses().addApiResponse(responseCode, apiResponse);
        } catch (Exception e) {
            log.error("Error processing ApiErrorResponse: responseCode={}", responseCode, e);
        }
    }

    private boolean mergeExamples(ApiResponse existingResponse, Map<String, Example> examples) {
        if (existingResponse == null || existingResponse.getContent() == null) {
            return false;
        }

        MediaType existingMediaType = existingResponse.getContent().get("application/json");
        if (existingMediaType == null) {
            return false;
        }

        if (existingMediaType.getExamples() == null) {
            existingMediaType.setExamples(new HashMap<>());
        }
        existingMediaType.getExamples().putAll(examples);
        return true;
    }
}
