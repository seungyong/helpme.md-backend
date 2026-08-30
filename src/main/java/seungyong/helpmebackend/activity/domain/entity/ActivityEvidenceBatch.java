package seungyong.helpmebackend.activity.domain.entity;

import java.util.List;

public record ActivityEvidenceBatch(List<Activity> items, long totalCount) {
    public ActivityEvidenceBatch {
        items = List.copyOf(items);
    }
}
