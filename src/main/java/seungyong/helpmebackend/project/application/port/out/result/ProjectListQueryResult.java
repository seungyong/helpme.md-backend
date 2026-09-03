package seungyong.helpmebackend.project.application.port.out.result;

import seungyong.helpmebackend.project.domain.entity.ProjectList;

import java.util.List;

public record ProjectListQueryResult(
        List<ProjectList.Item> items,
        String nextCursor,
        boolean hasNext
) {
    public ProjectListQueryResult {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
