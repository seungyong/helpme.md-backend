package seungyong.helpmebackend.user.domain.entity;

import seungyong.helpmebackend.user.domain.type.PlanCode;

import java.time.OffsetDateTime;
import java.util.Objects;

public record UserPlan(
        PlanCode code,
        short projectLimit,
        OffsetDateTime expiresAt
) {
    public UserPlan {
        Objects.requireNonNull(code, "플랜 코드는 null일 수 없습니다.");
        if (projectLimit < 1) {
            throw new IllegalArgumentException("프로젝트 한도는 1 이상이어야 합니다.");
        }
    }

    public static UserPlan free() {
        return new UserPlan(PlanCode.FREE, (short) 1, null);
    }
}
