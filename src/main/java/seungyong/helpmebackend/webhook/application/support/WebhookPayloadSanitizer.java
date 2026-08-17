package seungyong.helpmebackend.webhook.application.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.webhook.domain.exception.WebhookErrorCode;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebhookPayloadSanitizer {
    private final ObjectMapper objectMapper;

    public SanitizedPayload sanitize(String eventName, byte[] rawBody) {
        try {
            JsonNode source = objectMapper.readTree(rawBody);
            // repository.id와 installation.id는 필수 필드이므로, 없으면 예외 발생
            long repositoryId = requiredLong(source.path("repository"), "id");
            long installationId = requiredLong(source.path("installation"), "id");

            // eventName에 따라 필요한 필드만 복사하여 새로운 JSON 객체 생성 (원본 JSON은 그대로 유지)
            ObjectNode target = objectMapper.createObjectNode();
            copyCommon(source, target, repositoryId, installationId);

            switch (eventName) {
                case "push" -> copyPush(source, target);
                case "pull_request" -> copyPullRequest(source, target);
                case "ping" -> copyPing(source, target);
                default -> target.put("event", eventName);
            }

            Map<String, Object> payload = objectMapper.convertValue(target, Map.class);
            return new SanitizedPayload(
                    repositoryId,
                    installationId,
                    source.path("action").isTextual() ? source.path("action").asText() : null,
                    payload
            );
        } catch (CustomException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CustomException(WebhookErrorCode.INVALID_WEBHOOK_PAYLOAD);
        }
    }

    /**
     * 공통 필드 복사
     * - repository.id : repositioy ID
     * - repository.full_name : repository full name
     * - repository.private : repository private 여부
     * - installation.id : installation ID
     * - sender.login : 발생한 사용자 로그인
     * - action : action (push, pull_request 등)
     */
    private void copyCommon(
            JsonNode source, ObjectNode target, long repositoryId, long installationId
    ) {
        /*
        {
            "repository": {
                "id": 123,
                        "full_name": "seungyong/helpme-backend",
                        "private": true
            },
            "installation": {
                "id": 999
            },
            "sender": {
                "login": "seungyong"
            },
            "action": "opened"
        }
        */
        ObjectNode repository = target.putObject("repository");
        repository.put("id", repositoryId);
        copyText(source.path("repository"), repository, "full_name");
        copyBoolean(source.path("repository"), repository, "private");
        target.putObject("installation").put("id", installationId);
        ObjectNode sender = target.putObject("sender");
        copyText(source.path("sender"), sender, "login");
        copyText(source, target, "action");
    }

    /**
     * push 이벤트의 경우, commits 배열과 ref, before, after 등의 필드를 복사
     * - commits : 커밋 정보 배열
     * - ref : 브랜치 정보
     * - before : 이전 커밋 SHA
     * - after : 이후 커밋 SHA
     * - created : 브랜치 생성 여부
     * - deleted : 브랜치 삭제 여부
     * - forced : 강제 푸시 여부
     */
    private void copyPush(JsonNode source, ObjectNode target) {
        /*
        {
            "ref": "refs/heads/main",
            "before": "abc123",
            "after": "def456",
            "created": false,
            "deleted": false,
            "forced": false,
            "commits": [
                {
                    "id": "def456",
                    "message": "feat: add login",
                    "timestamp": "2026-08-17T...",
                    "url": "...",
                    "distinct": true,
                    "author": {
                        "username": "seungyong"
                    }
                }
            ]
         }
         */
        for (String field : new String[]{"ref", "before", "after"}) {
            copyText(source, target, field);
        }
        for (String field : new String[]{"created", "deleted", "forced"}) {
            copyBoolean(source, target, field);
        }
        ArrayNode commits = target.putArray("commits");
        if (source.path("commits").isArray()) {
            for (JsonNode commit : source.path("commits")) {
                ObjectNode item = commits.addObject();
                copyText(commit, item, "id");
                copyText(commit, item, "message");
                copyText(commit, item, "timestamp");
                copyText(commit, item, "url");
                copyBoolean(commit, item, "distinct");
                ObjectNode author = item.putObject("author");
                copyText(commit.path("author"), author, "username");
            }
        }
    }

    /**
     * pull_request 이벤트의 경우, pull_request 객체와 head, base, user 등의 필드를 복사
     * - pull_request.number : PR 번호
     * - pull_request.state : PR 상태 (open, closed 등)
     * - pull_request.title : PR 제목
     * - pull_request.html_url : PR URL
     * - pull_request.created_at : PR 생성일
     * - pull_request.updated_at : PR 수정일
     * - pull_request.closed_at : PR 종료일
     * - pull_request.merged_at : PR 병합일
     * - pull_request.draft : PR draft 여부
     * - pull_request.merged : PR 병합 여부
     * - pull_request.additions : 추가된 라인 수
     * - pull_request.deletions : 삭제된 라인 수
     * - pull_request.changed_files : 변경된 파일 수
     * - pull_request.head.ref : head 브랜치 이름
     * - pull_request.head.sha : head 커밋 SHA
     * - pull_request.base.ref : base 브랜치 이름
     * - pull_request.base.sha : base 커밋 SHA
     * - pull_request.user.login : PR 작성자 로그인
     */
    private void copyPullRequest(JsonNode source, ObjectNode target) {
        /*
        {
            "pull_request": {
                "number": 1,
                "state": "open",
                "title": "feat: add login",
                "html_url": "...",
                "created_at": "2026-08-17T...",
                "updated_at": "2026-08-17T...",
                "closed_at": null,
                "merged_at": null,
                "draft": false,
                "merged": false,
                "additions": 10,
                "deletions": 2,
                "changed_files": 3,
                "head": {
                    "ref": "feature/login",
                    "sha": "abc123"
                },
                "base": {
                    "ref": "main",
                    "sha": "def456"
                },
                "user": {
                    "login": "seungyong"
                }
            }
        }
         */
        JsonNode pullRequest = source.path("pull_request");
        if (!pullRequest.isObject()) {
            throw new CustomException(WebhookErrorCode.INVALID_WEBHOOK_PAYLOAD);
        }
        ObjectNode pr = target.putObject("pull_request");
        copyNumber(pullRequest, pr, "number");
        for (String field : new String[]{"state", "title", "html_url", "created_at", "updated_at", "closed_at", "merged_at"}) {
            copyText(pullRequest, pr, field);
        }
        for (String field : new String[]{"draft", "merged"}) {
            copyBoolean(pullRequest, pr, field);
        }
        for (String field : new String[]{"additions", "deletions", "changed_files"}) {
            copyNumber(pullRequest, pr, field);
        }
        copyRef(pullRequest.path("head"), pr.putObject("head"));
        copyRef(pullRequest.path("base"), pr.putObject("base"));
        ObjectNode user = pr.putObject("user");
        copyText(pullRequest.path("user"), user, "login");
    }

    /**
     * ping 이벤트의 경우, hook_id와 zen 필드를 복사
     * Webhook 설정 후 테스트 용으로 발생하는 이벤트
     * - hook_id : webhook ID
     * - zen : webhook 테스트 메시지
     */
    private void copyPing(JsonNode source, ObjectNode target) {
        /*
        {
            "hook_id": 123456,
            "zen": "Keep it logically awesome."
        }
         */
        copyNumber(source, target, "hook_id");
        copyText(source, target, "zen");
    }

    private void copyRef(JsonNode source, ObjectNode target) {
        copyText(source, target, "ref");
        copyText(source, target, "sha");
    }

    private long requiredLong(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.canConvertToLong()) {
            throw new CustomException(WebhookErrorCode.INVALID_WEBHOOK_PAYLOAD);
        }
        return value.asLong();
    }

    private void copyText(JsonNode source, ObjectNode target, String field) {
        if (source.path(field).isTextual()) {
            target.put(field, source.path(field).asText());
        }
    }

    private void copyBoolean(JsonNode source, ObjectNode target, String field) {
        if (source.path(field).isBoolean()) {
            target.put(field, source.path(field).asBoolean());
        }
    }

    private void copyNumber(JsonNode source, ObjectNode target, String field) {
        if (source.path(field).isNumber()) {
            target.put(field, source.path(field).asLong());
        }
    }

    public record SanitizedPayload(
            long repositoryId,
            long installationId,
            String action,
            Map<String, Object> value
    ) {
    }
}
