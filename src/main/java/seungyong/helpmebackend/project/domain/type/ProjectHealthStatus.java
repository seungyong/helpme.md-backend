package seungyong.helpmebackend.project.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectHealthStatus {
    HEALTHY("healthy"),
    SYNCING("syncing"),
    NO_EVENTS("no_events"),
    WARNING("warning");

    private final String apiValue;
}
