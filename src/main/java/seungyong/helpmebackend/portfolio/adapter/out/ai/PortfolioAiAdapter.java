package seungyong.helpmebackend.portfolio.adapter.out.ai;

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
import seungyong.helpmebackend.portfolio.adapter.out.ai.dto.PortfolioGenerationSchema;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioGenerationPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.result.GeneratedPortfolio;
import seungyong.helpmebackend.portfolio.domain.entity.Portfolio;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDocument;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceSnapshot;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioErrorCode;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioGenerationException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PortfolioAiAdapter implements PortfolioGenerationPortOut {
    private static final String SYSTEM_PROMPT = """
            당신은 개발자의 저장된 회고를 근거로 포트폴리오를 작성하는 편집자입니다.
            입력에 없는 성과, 수치, 기술, 링크를 만들어내지 마세요.
            모든 문단은 evidenceRefs로 입력 근거를 연결하고 JSON Schema를 지키세요.
            """;

    private final ChatModel chatModel;

    @Value("${spring.ai.openai.chat.cache-key.portfolio.prefix:portfolio}")
    private String portfolioCacheKeyPrefix;

    @Override
    public GeneratedPortfolio generate(Portfolio portfolio) {
        try {
            BeanOutputConverter<PortfolioGenerationSchema> converter =
                    new BeanOutputConverter<>(PortfolioGenerationSchema.class);
            Prompt prompt = new Prompt(
                    List.of(new SystemMessage(SYSTEM_PROMPT), new UserMessage(userPrompt(portfolio))),
                    OpenAiChatOptions.builder()
                            .responseFormat(new ResponseFormat(
                                    ResponseFormat.Type.JSON_SCHEMA, converter.getJsonSchema()
                            ))
                            .promptCacheKey(portfolioCacheKeyPrefix + ":" + portfolio.tone().getDatabaseValue())
                            .build()
            );
            ChatResponse response = chatModel.call(prompt);
            PortfolioGenerationSchema generated = converter.convert(
                    response.getResult().getOutput().getText()
            );
            if (generated == null || generated.sections() == null || generated.sections().isEmpty()) {
                throw new IllegalStateException("empty portfolio response");
            }
            Set<String> allowed = allowedEvidenceRefs(portfolio.sourceSnapshot());
            boolean invalid = generated.sections().stream()
                    .flatMap(section -> section.evidenceRefs() == null
                            ? java.util.stream.Stream.empty() : section.evidenceRefs().stream())
                    .anyMatch(ref -> !allowed.contains(ref));
            if (invalid) throw new IllegalStateException("unknown portfolio evidence ref");

            PortfolioDocument document = new PortfolioDocument(
                    PortfolioDocument.CURRENT_SCHEMA_VERSION,
                    generated.sections().stream().map(section -> new PortfolioDocument.Section(
                            section.id(), section.type(), section.title(), section.contentMd(), section.evidenceRefs()
                    )).toList()
            );
            return new GeneratedPortfolio(document);
        } catch (RuntimeException exception) {
            PortfolioErrorCode code = isRateLimited(exception)
                    ? PortfolioErrorCode.PORTFOLIO_RATE_LIMIT_EXCEEDED
                    : PortfolioErrorCode.PORTFOLIO_GENERATION_FAILED;
            throw new PortfolioGenerationException(code.getErrorCode(), code.getMessage(), exception);
        }
    }

    private String userPrompt(Portfolio portfolio) {
        String reflections = portfolio.sourceSnapshot().reflections().stream().map(item -> """
                - ref: reflection:%s
                  kind: %s
                  period: %s ~ %s
                  title: %s
                  content: %s
                """.formatted(item.id(), item.kind().getDatabaseValue(), item.periodStart(), item.periodEnd(),
                safe(item.title()), item.content().sections().stream()
                        .map(PortfolioAiAdapter::sectionText).collect(Collectors.joining("\n"))))
                .collect(Collectors.joining());
        String activities = portfolio.sourceSnapshot().activities().stream().map(item -> """
                - ref: activity:%s
                  title: %s
                  label: %s
                  publicUrl: %s
                """.formatted(item.id(), safe(item.title()), safe(item.label()), safe(item.publicUrl())))
                .collect(Collectors.joining());
        String links = java.util.stream.IntStream.range(0, portfolio.sourceSnapshot().customLinks().size())
                .mapToObj(index -> {
                    PortfolioSourceSnapshot.CustomLink link = portfolio.sourceSnapshot().customLinks().get(index);
                    return "- ref: custom_link:%d\n  label: %s\n  url: %s\n"
                            .formatted(index, link.label(), link.url());
                }).collect(Collectors.joining());
        return """
                title: %s
                period: %s ~ %s
                tone: %s

                saved reflections:
                %s

                public activities:
                %s

                custom public links:
                %s
                """.formatted(portfolio.title(), portfolio.periodStart(), portfolio.periodEnd(),
                portfolio.tone().getDatabaseValue(), reflections, activities, links);
    }

    private Set<String> allowedEvidenceRefs(PortfolioSourceSnapshot source) {
        Set<String> allowed = new HashSet<>();
        source.reflections().forEach(item -> allowed.add("reflection:" + item.id()));
        source.activities().forEach(item -> allowed.add("activity:" + item.id()));
        for (int index = 0; index < source.customLinks().size(); index++) {
            allowed.add("custom_link:" + index);
        }
        return allowed;
    }

    private boolean isRateLimited(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof RestClientResponseException response
                    && response.getStatusCode().value() == 429) return true;
            current = current.getCause();
        }
        return false;
    }

    private static String sectionText(seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument.Section section) {
        return section.title() + ": " + section.contentMd();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
