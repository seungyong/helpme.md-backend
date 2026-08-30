package seungyong.helpmebackend.section.adapter.in.web.dto.response;

import seungyong.helpmebackend.section.domain.entity.Section;

import java.util.List;

public record ReadmeComponentsResponse(
        List<ReadmeComponentResponse> components
) {
    public static ReadmeComponentsResponse from(List<Section> sections) {
        return new ReadmeComponentsResponse(
                sections.stream().map(ReadmeComponentResponse::from).toList()
        );
    }
}
