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

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GPTClient {
    private static final String importantFilesSchema = """
            {
              "type": "object",
              "properties": {
                "items": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "path": { "type": "string", "description": "파일의 경로" }
                    },
                    "required": ["path"],
                    "additionalProperties": false
                  }
                }
              },
              "required": ["items"],
              "additionalProperties": false
            }
            """;

    private static final String evaluationSchema = """
            {
              "type": "object",
              "properties": {
                "rating": {
                  "type": "number",
                  "description": "0.0에서 5.0 사이의 평가 점수"
                },
                "contents": {
                  "type": "array",
                  "items": {
                    "type": "string",
                    "description": "평가에 대한 구체적인 피드백 내용"
                  }
                }
              },
              "required": ["rating", "contents"],
              "additionalProperties": false
            }
            """;

    private final OpenAiChatModel openAiChatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.ai.openai.chat.cache-key.reviewer.prefix}")
    private String reviewerCacheKeyPrefix;

    @Value("${spring.ai.openai.chat.cache-key.important.prefix}")
    private String importantCacheKeyPrefix;

    public List<ImportantFile> importantFiles(List<RepositoryTreeResult> trees) throws JsonProcessingException {
        String systemPrompt = """
                너는 이제부터 유명한 소프트웨어 엔지니어이자 깃허브 전문가야.
                사용자가 제공하는 깃허브 저장소의 파일 트리 목록을 분석해서, 그 저장소에서 가장 중요한 파일이라고 생각되는 것을 골라야해.
                단, 디렉토리, 빈 파일, 이미지 파일 등은 소스 코드 분석에 도움이 되지 않으니 제외해야해.
                
                사용자가 제공하는 파일 트리 목록은 다음과 같아:
                [파일 경로, 파일 유형, 파일 URL], [...]
                
                중요한 파일을 고를 때는 다음 기준을 따라야해:
                1. 프로젝트의 핵심 기능과 관련된 파일 (예: 주요 소스 코드 파일, 핵심 모듈 등)
                - 프로젝트가 어떤 기능을 가지고 있는지 유추할 수 있게 하는 항목
                2. 의존성 파일 (예: package.json, pom.xml, gradle.build 등)
                - 프로젝트가 어떤 외부 라이브러리나 프레임워크를 사용하는지 알 수 있게 하는 항목
                3. 문서 파일 (예: README.md, CONTRIBUTING.md 등)
                - 프로젝트의 목적, 사용법, 기여 방법 등을 이해하는 데 도움이 되는 항목
                4. 프로젝트의 구조와 흐름을 이해하는 데 도움이 되는 파일 (예: 주요 클래스 파일, 모듈 파일, 디자인 패턴 등)
                - 프로젝트가 어떻게 구성되어 있는지 알 수 있게 하고, 흐름을 파악하는 데 도움이 되는 항목
                
                최소 5개 이상 선정해야 하고, 많아도 상관 없어.
                """;

        StringBuilder treeBuilder = new StringBuilder();
        for (RepositoryTreeResult tree : trees) {
            treeBuilder.append("[")
                    .append(tree.path()).append(", ")
                    .append(tree.type()).append("], ");
        }

        // 마지막 ", " 제거 (Math.max는 빈 문자열일 경우를 대비)
        treeBuilder.setLength(Math.max(treeBuilder.length() - 2, 0));

        List<Message> messages = List.of(
                // System 메시지 캐싱
                new SystemMessage(systemPrompt),
                new UserMessage("파일 트리 목록: " + treeBuilder)
        );

        Prompt prompt = new Prompt(messages,
                OpenAiChatOptions.builder()
                        .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, importantFilesSchema))
                        .promptCacheKey(importantCacheKeyPrefix)
                        .build());

        ChatResponse response = openAiChatModel.call(prompt);
        String json = response.getResult().getOutput().getText();

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
        String systemPrompt = """
                너는 이제부터 유명한 소프트웨어 엔지니어이자 깃허브 전문가야.
                사용자가 제공하는 Markdown 형식의 README.md 파일의 내용을 분석해서, 그 저장소에 대한 평가를 내려야해.
                평가 점수는 0.0에서 5.0 사이의 실수로 표현해야 하고, 구체적인 피드백 내용도 함께 제공해야해.
                말투는 존댓말 형태가 아닌 요약 형태로 작성해야하고, 종합적인 평가는 필요 없어.
                
                사용자가 너에게 제공하는 정보는 다음과 같아:
                1. README.md 파일의 내용
                2. 최근 커밋 메시지 목록 (200개 이내)
                3. 저장소의 파일 트리 목록
                4. 중요한 파일들 위치와 내용
                
                각 항목은 다음과 같은 형식(템플릿)으로 제공될거야:
                README.md 내용:
                <<<<README_START>>>>
                {README.md 파일 내용}
                <<<<README_END>>>>
                
                최근 커밋 메시지:
                [커밋 메시지, 커밋 메시지, ...]
                
                파일 트리 목록:
                [파일 경로, 파일 유형], [...]
                
                중요한 파일들:
                [파일 경로, 내용], [...]
                
                평가 점수는 다음 기준을 따라야해:
                1. README.md 내용이 프로젝트의 목적, 사용법 등을 명확하게 설명하고 있는가?
                2. README.md 내용이 커밋 메시지와 저장소의 구조를 잘 반영하고 있는가?
                3. README.md 내용이 깃허브 저장소의 일반적인 관례에 부합하는가?
                4. README.md 내용이 전체적으로 잘 작성되어 있는가?
                5. README.md 내용이 프로젝트의 가치를 잘 전달하고 있는가?
                6. README.md 내용이 사용자에게 유용한 정보를 제공하는가?
                7. README.md 내용이 시각적으로 잘 구성되어 있는가?
                8. README.md 내용이 최신 정보를 반영하고 있는가?
                9. README.md 내용이 오타나 문법 오류가 없는가?
                10. README.md 내용이 다른 유사한 프로젝트들과 비교했을 때 경쟁력이 있는가?
                
                구체적인 피드백 내용은 다음 기준을 따라야해:
                1. README.md 내용의 강점과 약점을 명확하게 지적해야해.
                2. 피드백 내용은 각 문제점에 대해 한 문장씩 구체적으로 작성해야해.
                3. 긍정적인 피드백과 건설적인 비판을 균형있게 제공해야해.
                4. 사용자가 쉽게 이해할 수 있도록 명확하고 간결하게 작성해야해.
                5. 피드백이 평가 점수와 일관되도록 해야해.
                6. 피드백이 저장소의 특성과 맥락을 고려해야해.
                7. 피드백이 실질적인 도움이 될 수 있도록 구체적이어야해.
                8. 피드백이 전문적이고 신뢰할 수 있어야해.
                9. 피드백이 긍정적인 동기를 부여할 수 있어야해.
                10. 피드백이 전체적인 평가와 조화를 이루어야해.
                
                위 기준들을 반드시 지켜서, README.md 파일에 대한 평가 점수와 구체적인 피드백 내용을 제공해줘.
                """;

        // 1. 커밋 목록 문자열 생성
        StringBuilder commitsBuilder = new StringBuilder();

        commitsBuilder.append("최근 커밋 메시지:\n[");

        for (String commit : commits) {
            commitsBuilder.append(commit).append(", ");
        }

        commitsBuilder.append("]\n");
        commitsBuilder.setLength(Math.max(commitsBuilder.length() - 2, 0));

        // 2. 파일 트리 목록 문자열 생성
        StringBuilder treeBuilder = new StringBuilder();

        treeBuilder.append("파일 트리 목록:\n[");
        for (RepositoryTreeResult tree : trees) {
            treeBuilder.append("[")
                    .append(tree.path()).append(", ")
                    .append(tree.type()).append("], ");
        }

        treeBuilder.append("]\n");
        treeBuilder.setLength(Math.max(treeBuilder.length() - 2, 0));

        // 3. 중요한 파일들 문자열 생성
        StringBuilder importantFilesBuilder = new StringBuilder();

        importantFilesBuilder.append("중요한 파일들:\n[");
        for (RepositoryFileContentResult file : importantFiles) {
            importantFilesBuilder.append("[")
                    .append(file.path()).append(", ")
                    .append(file.content().replace("\n", "\\n")).append("], ");
        }

        importantFilesBuilder.append("]\n");
        importantFilesBuilder.setLength(Math.max(importantFilesBuilder.length() - 2, 0));

        String fullPrompt = String.format(
                "README.md 내용:\n<<<<README_START>>>>\n%s\n<<<<README_END>>>>\n\n%s\n%s\n%s",
                readmeContent,
                commitsBuilder,
                treeBuilder,
                importantFilesBuilder
        );

        List<Message> messages = List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(fullPrompt)
        );

        Prompt prompt = new Prompt(messages,
                OpenAiChatOptions.builder()
                        .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, evaluationSchema))
                        .promptCacheKey(reviewerCacheKeyPrefix)
                        .build());

        ChatResponse response = openAiChatModel.call(prompt);
        String json = response.getResult().getOutput().getText();

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
}
