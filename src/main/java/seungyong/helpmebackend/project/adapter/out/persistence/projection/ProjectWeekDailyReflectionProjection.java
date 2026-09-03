package seungyong.helpmebackend.project.adapter.out.persistence.projection;

import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;

import java.time.LocalDate;

public interface ProjectWeekDailyReflectionProjection {
    LocalDate getPeriodStart();

    ReflectionStatus getStatus();

    Long getId();
}
