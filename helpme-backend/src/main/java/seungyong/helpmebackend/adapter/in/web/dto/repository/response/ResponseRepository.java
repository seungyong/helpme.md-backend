package seungyong.helpmebackend.adapter.in.web.dto.repository.response;

import java.util.List;

public record ResponseRepository(
    String content,
    String branches,
    String defaultBranch,
    String owner,
    String name,
    String avatarUrl,
    Evaluation evaluation,
    List<Component> components
) {
    public record Component(
            String id,
            String title,
            String content
    ) {}

    public record Evaluation(
            String status,
            Float rating,
            String content
    ) {}
}
