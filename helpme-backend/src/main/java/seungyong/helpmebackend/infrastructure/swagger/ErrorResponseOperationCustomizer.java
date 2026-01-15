package seungyong.helpmebackend.infrastructure.swagger;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import seungyong.helpmebackend.infrastructure.swagger.annotation.ApiErrorResponse;
import seungyong.helpmebackend.infrastructure.swagger.annotation.ApiErrorResponses;

import java.util.Arrays;
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

        if (annotation == null) {
            return;
        }

        for (ApiErrorResponse errorResponse : annotation.value()) {
            addErrorResponseToOperation(operation, errorResponse);
        }
    }

    /**
     * Operation에 에러 응답 추가
     */
    private void addErrorResponseToOperation(Operation operation, ApiErrorResponse errorResponse) {
        try {
            Map<String, Example> examples = ErrorExampleGenerator.generateErrorExamples(
                    errorResponse.errorCodeClass(),
                    errorResponse.errorCodes()
            );

            if (examples.isEmpty()) {
                log.warn("No examples generated for: {}", Arrays.toString(errorResponse.errorCodes()));
                return;
            }

            MediaType mediaType = new MediaType()
                    // ErrorResponse 스키마 참조
                    .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                    .examples(examples);

            Content content = new Content()
                    .addMediaType("application/json", mediaType);

            ApiResponse apiResponse = new ApiResponse()
                    .description(errorResponse.description())
                    .content(content);

            operation.getResponses().addApiResponse(errorResponse.responseCode(), apiResponse);
        } catch (Exception e) {
            log.error("Error processing ApiErrorResponse: {}", errorResponse, e);
        }
    }
}
