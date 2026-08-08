package seungyong.helpmebackend.project.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import seungyong.helpmebackend.project.domain.type.ReflectionWeekday;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectSettingsTest {
    @Nested
    @DisplayName("프로젝트 설정 생성")
    class Creation {
        @Test
        @DisplayName("Branch 목록을 방어적으로 복사")
        void success_defensiveCopy() {
            List<String> branches = new ArrayList<>(List.of("main", "develop"));

            ProjectSettings settings = settings(branches, false, (short) 30);
            branches.add("release");

            assertThat(settings.trackedBranches()).containsExactly("main", "develop");
            assertThatThrownBy(() -> settings.trackedBranches().add("release"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("전체 Branch 수집이면 선택 Branch 목록을 비움")
        void success_trackAllBranches() {
            ProjectSettings settings = settings(List.of("main"), true, (short) 30);

            assertThat(settings.trackAllBranches()).isTrue();
            assertThat(settings.trackedBranches()).isEmpty();
        }

        @Test
        @DisplayName("중복 또는 빈 Branch를 거부")
        void failure_invalidBranches() {
            assertThatThrownBy(() -> settings(List.of("main", "main"), false, (short) 30))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> settings(List.of("main", " "), false, (short) 30))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("서비스 허용 범위인 7~30일을 벗어난 payload 보관 기간을 거부")
        void failure_invalidRetention() {
            assertThatThrownBy(() -> settings(List.of("main"), false, (short) 6))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> settings(List.of("main"), false, (short) 31))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("payload 보관 기간의 경계값 7일과 30일을 허용")
        void success_retentionBoundaries() {
            assertThat(settings(List.of("main"), false, (short) 7)
                    .webhookPayloadRetentionDays()).isEqualTo((short) 7);
            assertThat(settings(List.of("main"), false, (short) 30)
                    .webhookPayloadRetentionDays()).isEqualTo((short) 30);
        }
    }

    private ProjectSettings settings(
            List<String> branches,
            boolean trackAllBranches,
            short retentionDays
    ) {
        return new ProjectSettings(
                branches,
                trackAllBranches,
                "Asia/Seoul",
                new ProjectSettings.DailyReflectionSchedule(true, LocalTime.of(23, 30)),
                new ProjectSettings.WeeklyReflectionSchedule(
                        true, ReflectionWeekday.SUNDAY, LocalTime.of(23, 50)
                ),
                retentionDays
        );
    }
}
