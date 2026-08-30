package seungyong.helpmebackend.section.adapter.in.web.dto.response;

import seungyong.helpmebackend.section.domain.entity.Section;

import java.time.OffsetDateTime;

public record ReadmeComponentResponse(
        Long id,
        String title,
        String content,
        Integer orderIdx,
        Integer version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ReadmeComponentResponse from(Section section) {
        return new ReadmeComponentResponse(
                section.getId(),
                section.getTitle(),
                section.getContent(),
                section.getOrderIdx(),
                section.getVersion(),
                section.getCreatedAt(),
                section.getUpdatedAt()
        );
    }
}
