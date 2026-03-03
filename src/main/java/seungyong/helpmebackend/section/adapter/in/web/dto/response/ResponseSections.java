package seungyong.helpmebackend.section.adapter.in.web.dto.response;

import java.util.List;

public record ResponseSections(
        List<Section> sections
) {
    public record Section(
            Long id,
            String title,
            String content,
            Short orderIdx
    ) { }
}
