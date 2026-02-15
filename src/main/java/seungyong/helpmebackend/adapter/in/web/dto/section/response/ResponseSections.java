package seungyong.helpmebackend.adapter.in.web.dto.section.response;

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
