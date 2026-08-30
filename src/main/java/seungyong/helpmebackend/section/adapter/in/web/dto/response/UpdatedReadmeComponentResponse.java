package seungyong.helpmebackend.section.adapter.in.web.dto.response;

import seungyong.helpmebackend.section.domain.entity.Section;

import java.time.OffsetDateTime;

public record UpdatedReadmeComponentResponse(
        Long id,
        String title,
        String content,
        Integer orderIdx,
        Integer version,
        OffsetDateTime updatedAt
) {
    public static UpdatedReadmeComponentResponse from(Section section) {
        return new UpdatedReadmeComponentResponse(
                section.getId(),
                section.getTitle(),
                section.getContent(),
                section.getOrderIdx(),
                section.getVersion(),
                section.getUpdatedAt()
        );
    }
}
