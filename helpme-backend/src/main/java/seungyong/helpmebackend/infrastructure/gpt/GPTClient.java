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
import seungyong.helpmebackend.adapter.out.result.RepositoryFileContentResult;
import seungyong.helpmebackend.adapter.out.result.RepositoryTreeResult;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.infrastructure.gpt.dto.EvaluationContent;
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

    public List<ImportantFile> importantFiles(List<RepositoryTreeResult> trees) throws JsonProcessingException {
        StringBuilder treeBuilder = new StringBuilder();
        for (RepositoryTreeResult tree : trees) {
            treeBuilder.append("[")
                    .append(tree.path()).append(", ")
                    .append(tree.type()).append("], ");
        }

        // 마지막 ", " 제거 (Math.max는 빈 문자열일 경우를 대비)
        treeBuilder.setLength(Math.max(treeBuilder.length() - 2, 0));

        List<Message> messages = buildMessages(
                GPTSystemPrompt.IMPORTANT_FILE_PROMPT,
                "파일 트리 목록:\n[" + treeBuilder + "]"
        );
        String json = getResponseText(GPTSchema.IMPORTANT_FILES_SCHEMA, importantCacheKeyPrefix, messages);

        JsonNode root = objectMapper.readTree(json);
        JsonNode itemsNode = root.get("items");

        if (itemsNode == null || !itemsNode.isArray()) {
            log.error("GPT important files response parsing error: items field is missing or empty. Full response = {}", json);
            throw new CustomException(GlobalErrorCode.GPT_ERROR);
        }

        return objectMapper.readerForListOf(ImportantFile.class)
                .readValue(itemsNode.toString());
    }

    public EvaluationContent evaluateReadme(
            String readmeContent,
            List<String> commits,
            List<RepositoryTreeResult> trees,
            List<RepositoryFileContentResult> importantFiles
    ) throws JsonProcessingException {
        // 1. 커밋 목록 문자열 생성
        StringBuilder commitsBuilder = new StringBuilder();

        commitsBuilder.append("최근 커밋 메시지:\n[");

        for (String commit : commits) {
            commitsBuilder.append(commit).append(", ");
        }

        commitsBuilder.setLength(Math.max(commitsBuilder.length() - 2, 0));
        commitsBuilder.append("]\n");

        // 2. 파일 트리 목록 문자열 생성
        StringBuilder treeBuilder = new StringBuilder();

        treeBuilder.append("파일 트리 목록:\n[");
        for (RepositoryTreeResult tree : trees) {
            treeBuilder.append("[")
                    .append(tree.path()).append(", ")
                    .append(tree.type()).append("], ");
        }

        treeBuilder.setLength(Math.max(treeBuilder.length() - 2, 0));
        treeBuilder.append("]\n");

        // 3. 중요한 파일들 문자열 생성
        StringBuilder importantFilesBuilder = new StringBuilder();

        importantFilesBuilder.append("중요한 파일들:\n[");
        for (RepositoryFileContentResult file : importantFiles) {
            importantFilesBuilder.append("[")
                    .append(file.path()).append(", ")
                    .append(file.content().replace("\n", "\\n")).append("], ");
        }

        importantFilesBuilder.setLength(Math.max(importantFilesBuilder.length() - 2, 0));
        importantFilesBuilder.append("]\n");

        String fullPrompt = String.format(
                "README.md 내용:\n<<<<README_START>>>>\n%s\n<<<<README_END>>>>\n\n%s\n%s\n%s",
                readmeContent,
                commitsBuilder,
                treeBuilder,
                importantFilesBuilder
        );

        List<Message> messages = buildMessages(GPTSystemPrompt.EVALUATION_PROMPT, fullPrompt);
        String json = getResponseText(GPTSchema.EVALUATION_SCHEMA, reviewerCacheKeyPrefix, messages);

        JsonNode root = objectMapper.readTree(json);
        JsonNode ratingNode = root.get("rating");
        JsonNode contentsNode = root.get("contents");

        if (ratingNode == null || contentsNode == null || !contentsNode.isArray()) {
            log.error("GPT evaluation response parsing error: missing fields. Full response = {}", json);
            throw new CustomException(GlobalErrorCode.GPT_ERROR);
        }

        float rating = ratingNode.floatValue();
        List<String> contents = objectMapper.readerForListOf(String.class)
                .readValue(contentsNode.toString());

        return new EvaluationContent(
                rating,
                contents
        );
    }

    private List<Message> buildMessages(String systemPrompt, String userPrompt) {
        return List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
        );
    }

    private String getResponseText(String schema, String promptCacheKey, List<Message> messages) {
        log.info("GPT Prompt: {}", messages.get(1).getText());

        Prompt prompt = new Prompt(messages,
                OpenAiChatOptions.builder()
                        .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, schema))
                        .promptCacheKey(promptCacheKey)
                        .build());

        ChatResponse response = openAiChatModel.call(prompt);

        log.info("""
                GPT used for tokens.
                Prompt tokens: {}
                Completion tokens: {}
                Total tokens: {}
                """,
                response.getMetadata().getUsage().getPromptTokens(),
                response.getMetadata().getUsage().getCompletionTokens(),
                response.getMetadata().getUsage().getTotalTokens());

        return response.getResult().getOutput().getText();
    }
}
