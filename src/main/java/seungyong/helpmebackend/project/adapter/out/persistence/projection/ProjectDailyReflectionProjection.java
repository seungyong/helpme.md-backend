package seungyong.helpmebackend.project.adapter.out.persistence.projection;

import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;

public interface ProjectDailyReflectionProjection {
    Long getId();

    ReflectionStatus getStatus();
}
