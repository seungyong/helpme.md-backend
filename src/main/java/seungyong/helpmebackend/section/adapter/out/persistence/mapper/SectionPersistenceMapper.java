package seungyong.helpmebackend.section.adapter.out.persistence.mapper;

import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.section.adapter.out.persistence.entity.SectionJpaEntity;
import seungyong.helpmebackend.section.domain.entity.Section;

public final class SectionPersistenceMapper {
    private SectionPersistenceMapper() {
    }

    public static Section toDomain(SectionJpaEntity entity) {
        return new Section(
                entity.getId(),
                entity.getProject().getId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getOrderIdx(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static SectionJpaEntity toEntity(Section section) {
        return SectionJpaEntity.builder()
                .id(section.getId())
                .project(ProjectJpaEntity.builder().id(section.getProjectId()).build())
                .title(section.getTitle())
                .content(section.getContent())
                .orderIdx(section.getOrderIdx())
                .version(section.getVersion())
                .createdAt(section.getCreatedAt())
                .updatedAt(section.getUpdatedAt())
                .build();
    }
}
