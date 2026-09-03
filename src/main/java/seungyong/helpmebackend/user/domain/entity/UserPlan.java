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

    /**
     * 무료 플랜을 생성합니다.
     *
     * @return 무료 플랜
     */
    public static UserPlan free() {
        return new UserPlan(PlanCode.FREE, (short) 1, null);
    }

    /**
     * 현재 시각 기준으로 유효한 프로젝트 한도를 반환합니다.
     * 만약 플랜이 FREE이거나, 만료된 플랜이라면 1을 반환합니다.
     *
     * @param now 기준 시각
     * @return 유효한 프로젝트 한도
     */
    public short effectiveProjectLimit(OffsetDateTime now) {
        Objects.requireNonNull(now, "기준 시각은 null일 수 없습니다.");
        if (code == PlanCode.FREE || expiresAt != null && !expiresAt.isAfter(now)) {
            return 1;
        }
        return projectLimit;
    }
}
