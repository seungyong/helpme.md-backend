package seungyong.helpmebackend.infrastructure.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

/**
 * OpenAPI 스펙에 ErrorResponse 스키마 추가 <br />
 * OpenApiCustomizer는 Swagger 설정 시점에 한 번만 호출됨
 */
@Slf4j
@Component
public class ErrorResponseOpenApiCustomizer implements OpenApiCustomizer {
    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }

        // ErrorResponse 스키마 골격을 만듦으로써, 각 Operation에 참조할 수 있도록 함
        Schema<?> errorResponseSchema = new Schema<>()
                .type("object")
                .addProperties("timestamp", new Schema<>().type("string").example("2023-10-05T14:48:00Z"))
                .addProperties("status", new Schema<>().type("integer").example(400))
                .addProperties("error", new Schema<>().type("string").example("BAD_REQUEST"))
                .addProperties("message", new Schema<>().type("string").example("Invalid request parameter"))
                .addProperties("code", new Schema<>().type("string").example("BAD_REQUEST"))
                .addProperties("errorCode", new Schema<>().type("string").example("VALID_400"));

        openApi.getComponents().addSchemas("ErrorResponse", errorResponseSchema);
    }
}
