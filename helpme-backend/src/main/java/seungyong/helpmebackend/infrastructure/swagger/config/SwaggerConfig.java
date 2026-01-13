package seungyong.helpmebackend.infrastructure.swagger.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import seungyong.helpmebackend.infrastructure.swagger.ErrorResponseOpenApiCustomizer;
import seungyong.helpmebackend.infrastructure.swagger.ErrorResponseOperationCustomizer;

import java.util.Arrays;

@Configuration
@EnableWebMvc
@RequiredArgsConstructor
public class SwaggerConfig {
    private final ErrorResponseOpenApiCustomizer errorResponseOpenApiCustomizer;
    private final ErrorResponseOperationCustomizer errorResponseOperationCustomizer;

    @Bean
    public OpenAPI config() {
        String jwt = "JWT";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwt);
        Components components = new Components()
                .addSecuritySchemes(jwt, new SecurityScheme()
                    .name(jwt)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                );

        return new OpenAPI()
                .info(apiInfo())
                .servers(Arrays.asList(
                        new Server().url("http://localhost:8080").description("개발 서버"),
                        new Server().url("https://#").description("운영 서버")
                ))
                .addSecurityItem(securityRequirement)
                .components(components);
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/**")
                .addOpenApiCustomizer(errorResponseOpenApiCustomizer)
                .addOperationCustomizer(errorResponseOperationCustomizer)
                .build();
    }

    private Info apiInfo() {
        return new Info()
                .title("Helpme.md API")
                .description("Helpme.md API Documents")
                .version("1.0.0");
    }
}
