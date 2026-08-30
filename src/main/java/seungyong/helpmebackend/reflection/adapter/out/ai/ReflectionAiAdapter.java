package seungyong.helpmebackend.reflection.adapter.out.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.reflection.adapter.out.ai.dto.ReflectionGenerationSchema;
import seungyong.helpmebackend.reflection.application.port.out.ReflectionGenerationPortOut;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionSourceSnapshot;
import seungyong.helpmebackend.reflection.domain.exception.ReflectionErrorCode;
import seungyong.helpmebackend.reflection.domain.exception.ReflectionGenerationException;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReflectionAiAdapter implements ReflectionGenerationPortOut {
    private static final String SYSTEM_PROMPT = """
            당신은 개발자가 수집된 사실을 바탕으로 회고를 작성하도록 돕는 편집자입니다.
            제공된 근거에 없는 성과, 수치, 원인, 감정을 만들어내지 마세요.
            각 section은 markdown 형식이며 type은 서버에서 markdown으로 고정합니다.
            evidenceRefs에는 해당 문장을 뒷받침하는 입력 ref만 넣으세요.
            일일 회고는 요약, 배운 점, 다음 행동을 중심으로 작성하세요.
            주간 회고는 성과, 문제 해결, 배운 점, 다음 주 계획을 중심으로 작성하세요.
            출력은 반드시 요청된 JSON Schema를 따르세요.
            """;

    private final ChatModel chatModel;

    @Value("${spring.ai.openai.chat.cache-key.reflection.prefix:reflection}")
    private String reflectionCacheKeyPrefix;

    @Override
    public GeneratedReflection generate(
            Project project,
            ReflectionKind kind,
            LocalDate periodStart,
            LocalDate periodEnd,
            ReflectionSourceSnapshot source
    ) {
        try {
            BeanOutputConverter<ReflectionGenerationSchema> converter =
                    new BeanOutputConverter<>(ReflectionGenerationSchema.class);
            Prompt prompt = new Prompt(
                    List.of(
                            new SystemMessage(SYSTEM_PROMPT),
                            new UserMessage(userPrompt(
                                    project, kind, periodStart, periodEnd, source
                            ))
                    ),
                    OpenAiChatOptions.builder()
                            .responseFormat(new ResponseFormat(
                                    ResponseFormat.Type.JSON_SCHEMA,
                                    converter.getJsonSchema()
                            ))
                            .promptCacheKey(
                                    reflectionCacheKeyPrefix + ":" + kind.getDatabaseValue()
                            )
                            .build()
            );
            ChatResponse response = chatModel.call(prompt);
            ReflectionGenerationSchema generated = converter.convert(
                    response.getResult().getOutput().getText()
            );
            if (generated == null || generated.title() == null
                    || generated.title().isBlank() || generated.sections() == null) {
                throw new IllegalStateException("empty reflection response");
            }
            Set<String> allowedEvidenceRefs = source.evidence().stream()
                    .map(ReflectionSourceSnapshot.Evidence::ref)
                    .collect(Collectors.toUnmodifiableSet());
            boolean invalidEvidenceRef = generated.sections().stream()
                    .flatMap(section -> section.evidenceRefs() == null
                            ? java.util.stream.Stream.empty()
                            : section.evidenceRefs().stream())
                    .anyMatch(ref -> !allowedEvidenceRefs.contains(ref));
            if (invalidEvidenceRef) {
                throw new IllegalStateException("unknown reflection evidence ref");
            }
            ReflectionDocument document = new ReflectionDocument(
                    ReflectionDocument.CURRENT_SCHEMA_VERSION,
                    generated.sections().stream()
                            .map(section -> new ReflectionDocument.Section(
                                    section.id(),
                                    "markdown",
                                    section.title(),
                                    section.contentMd(),
                                    section.evidenceRefs()
                            ))
                            .toList()
            );
            return new GeneratedReflection(generated.title(), document);
        } catch (RuntimeException exception) {
            boolean rateLimited = isRateLimited(exception);
            ReflectionErrorCode code = rateLimited
                    ? ReflectionErrorCode.REFLECTION_RATE_LIMIT_EXCEEDED
                    : ReflectionErrorCode.REFLECTION_GENERATION_FAILED;
            throw new ReflectionGenerationException(
                    code.getErrorCode(), code.getMessage(), true, exception
            );
        }
    }

    private String userPrompt(
            Project project,
            ReflectionKind kind,
            LocalDate periodStart,
            LocalDate periodEnd,
            ReflectionSourceSnapshot source
    ) {
        String evidence = source.evidence().stream()
                .map(item -> """
                        - ref: %s
                          title: %s
                          label: %s
                          content: %s
                        """.formatted(
                        item.ref(), safe(item.title()), safe(item.label()), safe(item.content())
                ))
                .collect(Collectors.joining());
        String daily = source.dailyReflections().stream()
                .map(item -> """
                        - date: %s
                          reflectionId: %s
                          included: %s
                          reason: %s
                          content: %s
                        """.formatted(
                        item.date(), item.reflectionId(), item.included(),
                        item.reason(), safe(item.content())
                ))
                .collect(Collectors.joining());
        return """
                repository: %s
                reflectionKind: %s
                period: %s ~ %s
                sourceQualityGap: %s

                evidence:
                %s

                dailyReflections:
                %s
                """.formatted(
                project.getRepoFullName(), kind.getDatabaseValue(),
                periodStart, periodEnd, source.collectionGap(), evidence, daily
        );
    }

    private boolean isRateLimited(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof RestClientResponseException response
                    && response.getStatusCode().value() == 429) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
