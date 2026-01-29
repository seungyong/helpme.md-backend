package seungyong.helpmebackend.infrastructure.sse;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SSETaskName {
    COMPLETION_EVALUATE_PUSH("completion-evaluate-push"),
    COMPLETION_EVALUATE_PUSH_ERROR("completion-evaluate-push-error"),
    COMPLETION_EVALUATE_DRAFT("completion-evaluate-draft"),
    COMPLETION_EVALUATE_DRAFT_ERROR("completion-evaluate-draft-error"),
    COMPLETION_GENERATE("completion-generate"),
    COMPLETION_GENERATE_ERROR("completion-generate-error"),

    ;

    private final String taskName;
}
