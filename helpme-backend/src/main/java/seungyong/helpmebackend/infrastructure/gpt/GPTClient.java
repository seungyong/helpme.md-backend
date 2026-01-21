package seungyong.helpmebackend.infrastructure.gpt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.adapter.out.command.EvaluationCommand;
import seungyong.helpmebackend.adapter.out.command.RepositoryImportantCommand;
import seungyong.helpmebackend.adapter.out.command.RepositoryInfoCommand;
import seungyong.helpmebackend.adapter.out.result.RepositoryFileContentResult;
import seungyong.helpmebackend.adapter.out.result.RepositoryLanguageResult;
import seungyong.helpmebackend.adapter.out.result.RepositoryTreeResult;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.infrastructure.gpt.dto.EvaluationContent;
import seungyong.helpmebackend.infrastructure.gpt.dto.GPTRepositoryInfo;
import seungyong.helpmebackend.infrastructure.gpt.dto.ImportantFile;
import seungyong.helpmebackend.infrastructure.gpt.type.GPTSchema;
import seungyong.helpmebackend.infrastructure.gpt.type.GPTSystemPrompt;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GPTClient {

    private final OpenAiChatModel openAiChatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.ai.openai.chat.cache-key.reviewer.prefix}")
    private String reviewerCacheKeyPrefix;

    @Value("${spring.ai.openai.chat.cache-key.important.prefix}")
    private String importantCacheKeyPrefix;

    @Value("${spring.ai.openai.chat.cache-key.repository-info.prefix}")
    private String repositoryInfoCacheKeyPrefix;

    public GPTRepositoryInfo getRepositoryInfo(String fullName, RepositoryInfoCommand repository) throws JsonProcessingException {
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

        String json = getResponseText(GPTSchema.REPOSITORY_ANALYZE_SCHEMA, repositoryInfoCacheKeyPrefix, messages, fullName);
        return objectMapper.readValue(json, GPTRepositoryInfo.class);
    }

    public List<ImportantFile> importantFiles(RepositoryImportantCommand command) throws JsonProcessingException {
        String languageList = languagesToString(command.repoInfo().languages());
        String latestCommits = listToString(command.repoInfo().commits().latestCommit(), "최근 커밋 메시지:\n");
        String middleCommits = listToString(command.repoInfo().commits().middleCommit(), "중간 커밋 메시지:\n");
        String oldCommits = listToString(command.repoInfo().commits().initialCommit(), "초기 커밋 메시지:\n");
        String trees = treeToString(command.repoInfo().trees());
        String entryPoints = listToString(
                command.entryPoints().stream()
                        .map(RepositoryFileContentResult::path)
                        .toList(),
                "진입점/설정 파일 또는 디렉토리 정보:\n"
        );
        String techStacks = listToString(
                List.of(command.techStack()),
                "기술 스택 및 프레임워크:\n"
        );
        String projectSize = "프로젝트 규모:\n" + command.projectSize() + "\n";

        List<Message> messages = buildMessages(
                GPTSystemPrompt.IMPORTANT_FILE_PROMPT,
                String.format(
                        "%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s",
                        languageList,
                        techStacks,
                        trees,
                        entryPoints,
                        projectSize,
                        latestCommits,
                        middleCommits,
                        oldCommits
                )
        );

        String json = getResponseText(GPTSchema.IMPORTANT_FILES_SCHEMA, importantCacheKeyPrefix, messages, command.fullName());
        JsonNode rootNode = objectMapper.readTree(json);
        JsonNode importantFilesNode = rootNode.get("items");

        if (importantFilesNode == null || !importantFilesNode.isArray()) {
            log.error("GPT important files response parsing error: items field is missing or empty. Full response = {}", json);
            throw new CustomException(GlobalErrorCode.GPT_ERROR);
        }

        return objectMapper.readerForListOf(ImportantFile.class).readValue(importantFilesNode.toString());
    }

    public EvaluationContent evaluateReadme(EvaluationCommand command) throws JsonProcessingException {
        // 1. 프로젝트 언어
        String languages = languagesToString(command.repoInfo().languages());

        // 2. 커밋 목록 문자열 생성
        String latestCommits = listToString(command.repoInfo().commits().latestCommit(), "최근 커밋 메시지:\n");
        String middleCommits = listToString(command.repoInfo().commits().middleCommit(), "중간 커밋 메시지:\n");
        String oldCommits = listToString(command.repoInfo().commits().initialCommit(), "초기 커밋 메시지:\n");

        // 3. 파일 트리 목록 문자열 생성
        String tree = treeToString(command.repoInfo().trees());

        // 4. 진입점/설정 파일 목록 문자열 생성
        String entryPoints = listToString(
                command.entryPoints().stream()
                        .map(RepositoryFileContentResult::path)
                        .toList(),
                "진입점/설정 파일 또는 디렉토리 정보:\n"
        );

        // 5. 중요한 파일들 문자열 생성
        String importantFiles = listToString(
                command.importantFiles().stream()
                        .map(RepositoryFileContentResult::path)
                        .toList(),
                "중요 파일:\n"
        );

        // 6. 기술 스택 및 프로젝트 규모 문자열 생성
        String techStacks = listToString(
                List.of(command.techStack()),
                "기술 스택 및 프레임워크:\n"
        );
        String projectSize = "프로젝트 규모:\n" + command.projectSize() + "\n";

        String fullPrompt = String.format(
                "README.md 내용:\n<<<<README_START>>>>\n%s\n<<<<README_END>>>>\n\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s",
                command.readmeContent(),
                languages,
                techStacks,
                tree,
                entryPoints,
                importantFiles,
                projectSize,
                latestCommits,
                middleCommits,
                oldCommits
        );

        List<Message> messages = buildMessages(GPTSystemPrompt.EVALUATION_PROMPT, fullPrompt);
        String json = getResponseText(GPTSchema.EVALUATION_SCHEMA, reviewerCacheKeyPrefix, messages, command.fullName());
        return objectMapper.readValue(json, EvaluationContent.class);
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

    private String getResponseText(String schema, String promptCacheKey, List<Message> messages, String fullName) {
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

        return response.getResult().getOutput().getText();
    }
}
