package seungyong.helpmebackend.infrastructure.gpt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.adapter.out.command.EvaluationCommand;
import seungyong.helpmebackend.adapter.out.command.GenerateReadmeCommand;
import seungyong.helpmebackend.adapter.out.command.RepositoryInfoCommand;
import seungyong.helpmebackend.adapter.out.result.*;
import seungyong.helpmebackend.infrastructure.gpt.dto.GPTDraftReadmeGenerationSchema;
import seungyong.helpmebackend.infrastructure.gpt.dto.GPTEvaluationSchema;
import seungyong.helpmebackend.infrastructure.gpt.dto.GPTRepositoryAnalyzeSchema;
import seungyong.helpmebackend.infrastructure.gpt.dto.PromptContext;
import seungyong.helpmebackend.infrastructure.gpt.type.GPTSystemPrompt;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GPTClient {

    private final OpenAiChatModel openAiChatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.ai.openai.chat.cache-key.reviewer.prefix}")
    private String reviewerCacheKeyPrefix;

    @Value("${spring.ai.openai.chat.cache-key.repository-info.prefix}")
    private String repositoryInfoCacheKeyPrefix;

    public GPTRepositoryInfoResult getRepositoryInfo(String fullName, RepositoryInfoCommand repository) {
        String languageList = languagesToString(repository.languages());
        String latestCommits = listToString(repository.commits().latestCommit(), "최근 커밋 메시지:\n");
        String middleCommits = listToString(repository.commits().middleCommit(), "중간 커밋 메시지:\n");
        String oldCommits = listToString(repository.commits().initialCommit(), "초기 커밋 메시지:\n");
        String trees = treeToString(repository.trees());

        List<Message> messages = buildMessages(
                GPTSystemPrompt.REPOSITORY_ANALYZE_PROMPT,
                String.format(
                        "%s\n%s\n%s\n%s\n%s",
                        languageList,
                        latestCommits,
                        middleCommits,
                        oldCommits,
                        trees
                )
        );

        BeanOutputConverter<GPTRepositoryAnalyzeSchema> converter = new BeanOutputConverter<>(GPTRepositoryAnalyzeSchema.class);
        GPTRepositoryAnalyzeSchema response = getResponseText(converter, repositoryInfoCacheKeyPrefix, messages, fullName);

        return new GPTRepositoryInfoResult(
                response.techStack(),
                response.projectSize().toString(),
                response.entryPoints(),
                response.importantFiles()
        );
    }

    public EvaluationContentResult evaluateReadme(EvaluationCommand command) {
        PromptContext ctx = generateSystemPromptContext(
                command.repoInfo(),
                command.entryPoints(),
                command.importantFiles(),
                command.techStack(),
                command.projectSize()
        );

        String systemPrompt = String.format(
                "README.md 내용:\n<<<<README_START>>>>\n%s\n<<<<README_END>>>>\n\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s",
                command.readmeContent(),
                ctx.languages(),
                ctx.techStacks(),
                ctx.tree(),
                ctx.entryPoints(),
                ctx.importantFiles(),
                ctx.projectSize(),
                ctx.latestCommits(),
                ctx.middleCommits(),
                ctx.oldCommits()
        );

        List<Message> messages = buildMessages(GPTSystemPrompt.EVALUATION_PROMPT, systemPrompt);
        BeanOutputConverter<GPTEvaluationSchema> converter = new BeanOutputConverter<>(GPTEvaluationSchema.class);
        GPTEvaluationSchema response = getResponseText(converter, reviewerCacheKeyPrefix, messages, command.fullName());

        return new EvaluationContentResult(
                response.rating(),
                Arrays.asList(response.contents())
        );
    }

    public String generateDraftReadme(GenerateReadmeCommand command) {
        PromptContext ctx = generateSystemPromptContext(
                command.repoInfo(),
                command.entryPoints(),
                command.importantFiles(),
                command.techStack(),
                command.projectSize()
        );

        String systemPrompt = String.format(
                "%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s",
                ctx.languages(),
                ctx.techStacks(),
                ctx.tree(),
                ctx.entryPoints(),
                ctx.importantFiles(),
                ctx.projectSize(),
                ctx.latestCommits(),
                ctx.middleCommits(),
                ctx.oldCommits()
        );

        List<Message> messages = buildMessages(GPTSystemPrompt.DRAFT_README_GENERATION_PROMPT, systemPrompt);
        BeanOutputConverter<GPTDraftReadmeGenerationSchema> converter = new BeanOutputConverter<>(GPTDraftReadmeGenerationSchema.class);

        GPTDraftReadmeGenerationSchema response = getResponseText(converter, reviewerCacheKeyPrefix, messages, command.fullName());
        return response.content();
    }

    private PromptContext generateSystemPromptContext(
            RepositoryInfoCommand repoInfo,
            List<RepositoryFileContentResult> entryPoints,
            List<RepositoryFileContentResult> importantFiles,
            String[] techStack,
            String projectSize
    ) {
        String languages = languagesToString(repoInfo.languages());

        String latestCommits = listToString(
                repoInfo.commits().latestCommit(),
                "최근 커밋 메시지:\n"
        );
        String middleCommits = listToString(
                repoInfo.commits().middleCommit(),
                "중간 커밋 메시지:\n"
        );
        String oldCommits = listToString(
                repoInfo.commits().initialCommit(),
                "초기 커밋 메시지:\n"
        );

        String tree = treeToString(repoInfo.trees());

        String entryPointsStr = listToString(
                entryPoints.stream()
                        .map(RepositoryFileContentResult::path)
                        .toList(),
                "진입점/설정 파일 또는 디렉토리 정보:\n"
        );

        String importantFilesStr = listToString(
                importantFiles.stream()
                        .map(RepositoryFileContentResult::path)
                        .toList(),
                "중요 파일:\n"
        );

        String techStacks = listToString(
                List.of(techStack),
                "기술 스택 및 프레임워크:\n"
        );

        String projectSizeStr = "프로젝트 규모:\n" + projectSize + "\n";

        return new PromptContext(
                languages,
                latestCommits,
                middleCommits,
                oldCommits,
                tree,
                entryPointsStr,
                importantFilesStr,
                techStacks,
                projectSizeStr
        );
    }

    private String languagesToString(List<RepositoryLanguageResult> languages) {
        StringBuilder builder = new StringBuilder();
        builder.append("언어 사용 비율:\n[");

        for (RepositoryLanguageResult language : languages) {
            builder.append("{")
                    .append(language.name())
                    .append(": ")
                    .append(language.bytes())
                    .append("}, ");
        }

        if (!languages.isEmpty()) {
            builder.setLength(builder.length() - 2); // 마지막 ", " 제거
        }

        builder.append("]");
        return builder.toString();
    }

    private String listToString(List<String> list, String prefix) {
        StringBuilder builder = new StringBuilder();

        builder.append(prefix).append("[");
        for (String item : list) {
            builder.append(item).append(", ");
        }

        if (!list.isEmpty()) {
            builder.setLength(builder.length() - 2); // 마지막 ", " 제거
        }

        builder.append("]");
        return builder.toString();
    }

    private String treeToString(List<RepositoryTreeResult> trees) {
        StringBuilder builder = new StringBuilder();
        builder.append("파일 트리 목록:\n[");

        for (RepositoryTreeResult tree : trees) {
            builder.append("[")
                    .append(tree.path()).append(", ")
                    .append(tree.type()).append("], ");
        }
        builder.setLength(Math.max(builder.length() - 2, 0)); // 마지막 ", " 제거
        builder.append("]\n");

        return builder.toString();
    }

    private List<Message> buildMessages(String systemPrompt, String userPrompt) {
        return List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
        );
    }

    private <T> T getResponseText(BeanOutputConverter<T> converter, String promptCacheKey, List<Message> messages, String fullName) {
        String schema = converter.getJsonSchema();

        Prompt prompt = new Prompt(messages,
                OpenAiChatOptions.builder()
                        .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, schema))
                        .promptCacheKey(promptCacheKey)
                        .build());

        ChatResponse response = openAiChatModel.call(prompt);

        // 로그에 토큰 사용량 출력
        log.info("""
                GPT Response Token Usage for repository: {}
                Prompt tokens: {}
                Completion tokens: {}
                Total tokens: {}
                """,
                fullName,
                response.getMetadata().getUsage().getPromptTokens(),
                response.getMetadata().getUsage().getCompletionTokens(),
                response.getMetadata().getUsage().getTotalTokens());

        String responseText = response.getResult().getOutput().getText();
        return converter.convert(responseText);
    }
}
