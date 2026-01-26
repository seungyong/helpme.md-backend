package seungyong.helpmebackend.adapter.in.web.dto.component.response;

import java.util.List;

public record ResponseComponents(
        List<Component> components
) {
    public record Component(
            Long id,
            String title,
            String content
    ) {
    }
}
