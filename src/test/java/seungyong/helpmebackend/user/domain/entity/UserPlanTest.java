package seungyong.helpmebackend.user.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seungyong.helpmebackend.user.domain.type.PlanCode;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserPlanTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-01T00:00:00Z");

    @Test
    @DisplayName("유효한 유료 플랜은 설정한 프로젝트 한도를 사용")
    void effectiveLimit_pro() {
        UserPlan plan = new UserPlan(PlanCode.PRO, (short) 3, NOW.plusDays(1));

        assertThat(plan.effectiveProjectLimit(NOW)).isEqualTo((short) 3);
    }

    @Test
    @DisplayName("무료 또는 만료된 플랜은 기존 데이터를 삭제하지 않고 한도를 1로 계산")
    void effectiveLimit_freeOrExpired() {
        UserPlan free = new UserPlan(PlanCode.FREE, (short) 5, null);
        UserPlan expired = new UserPlan(PlanCode.PRO, (short) 5, NOW);

        assertThat(free.effectiveProjectLimit(NOW)).isEqualTo((short) 1);
        assertThat(expired.effectiveProjectLimit(NOW)).isEqualTo((short) 1);
    }
}
