package seungyong.helpmebackend.reflection.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.type.ReflectionWeekday;
import seungyong.helpmebackend.reflection.application.port.in.command.CreateReflectionCommand;
import seungyong.helpmebackend.reflection.domain.exception.ReflectionErrorCode;
import seungyong.helpmebackend.reflection.domain.type.ReflectionGenerationMode;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReflectionScheduleWorker {
    private final ProjectPortOut projectPortOut;
    private final ReflectionService reflectionService;

    /**
     * 스케줄러를 통해 활성화된 모든 프로젝트에 대해 회고 생성을 예약합니다.
     * - 일일 회고: 프로젝트 설정에 따라 매일 지정된 시간 이후에 생성
     * - 주간 회고: 프로젝트 설정에 따라 매주 지정된 요일 이후에 생성
     */
    @Scheduled(fixedDelayString = "${workers.reflection.schedule-delay-ms:60000}")
    public void enqueueDueReflections() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        projectPortOut.getAllActive().forEach(project -> enqueue(project, now));
    }

    /**
     * 프로젝트 설정에 따라 회고 생성을 예약합니다.
     * @param project 프로젝트 엔티티
     * @param now 현재 시간 (UTC)
     */
    void enqueue(Project project, OffsetDateTime now) {
        ZonedDateTime localNow = now.atZoneSameInstant(
                ZoneId.of(project.getSettings().timezone())
        );
        if (project.getSettings().daily().enabled()) {
            LocalDate dailyDate = latestDailyDate(project, localNow);
            create(project, ReflectionKind.DAILY, dailyDate);
        }
        if (project.getSettings().weekly().enabled()) {
            LocalDate weeklyEnd = latestWeeklyEnd(project, localNow);
            create(project, ReflectionKind.WEEKLY, weeklyEnd.minusDays(6));
        }
    }

    /**
     * 프로젝트 설정에 따라 가장 최근의 일일 회고 날짜를 계산합니다.
     * @param project 프로젝트 엔티티
     * @param now 현재 시간 (프로젝트 로컬 타임존)
     * @return 가장 최근의 일일 회고 날짜
     */
    private LocalDate latestDailyDate(Project project, ZonedDateTime now) {
        LocalDate candidate = now.toLocalDate();
        if (now.toLocalTime().isBefore(project.getSettings().daily().generationTime())) {
            candidate = candidate.minusDays(1);
        }
        return candidate;
    }

    /**
     * 프로젝트 설정에 따라 가장 최근의 주간 회고 종료 날짜를 계산합니다.
     * @param project 프로젝트 엔티티
     * @param now 현재 시간 (프로젝트 로컬 타임존)
     * @return 가장 최근의 주간 회고 종료 날짜
     */
    private LocalDate latestWeeklyEnd(Project project, ZonedDateTime now) {
        ReflectionWeekday configured = project.getSettings().weekly().generationDay();
        DayOfWeek day = configured.getDatabaseValue() == 0
                ? DayOfWeek.SUNDAY : DayOfWeek.of(configured.getDatabaseValue());
        LocalDate candidate = now.toLocalDate().with(TemporalAdjusters.previousOrSame(day));
        if (candidate.equals(now.toLocalDate())
                && now.toLocalTime().isBefore(
                project.getSettings().weekly().generationTime())) {
            candidate = candidate.minusWeeks(1);
        }
        return candidate;
    }

    /**
     * 회고 생성을 시도합니다. 이미 존재하는 경우에는 무시합니다.
     * @param project 프로젝트 엔티티
     * @param kind 회고 종류 (일일/주간)
     * @param periodStart 회고 기간 시작 날짜
     */
    private void create(
            Project project, ReflectionKind kind, LocalDate periodStart
    ) {
        try {
            reflectionService.createReflection(new CreateReflectionCommand(
                    project.getUserId(),
                    project.getId(),
                    kind.getDatabaseValue(),
                    periodStart,
                    ReflectionGenerationMode.AI.getApiValue(),
                    true
            ));
        } catch (CustomException exception) {
            if (exception.getErrorCode()
                    != ReflectionErrorCode.REFLECTION_SOURCE_INSUFFICIENT) {
                log.warn(
                        "Scheduled reflection enqueue failed: projectId={}, kind={}, code={}",
                        project.getId(), kind, exception.getErrorCode().getErrorCode()
                );
            }
        } catch (RuntimeException exception) {
            log.warn(
                    "Scheduled reflection enqueue failed: projectId={}, kind={}, exceptionType={}",
                    project.getId(), kind, exception.getClass().getSimpleName()
            );
        }
    }
}
