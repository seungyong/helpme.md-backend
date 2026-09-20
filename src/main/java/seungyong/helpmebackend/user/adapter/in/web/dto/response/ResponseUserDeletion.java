package seungyong.helpmebackend.user.adapter.in.web.dto.response;

import seungyong.helpmebackend.user.application.port.in.result.UserDeletionResult;

import java.time.OffsetDateTime;

public record ResponseUserDeletion(
        String status,
        OffsetDateTime requestedAt,
        String requiredAction
) {
    public static ResponseUserDeletion from(UserDeletionResult result) {
        return new ResponseUserDeletion(
                result.status(), result.requestedAt(), result.requiredAction()
        );
    }
}
