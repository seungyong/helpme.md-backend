package seungyong.helpmebackend.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import seungyong.helpmebackend.adapter.out.persistence.entity.SectionJpaEntity;
import seungyong.helpmebackend.domain.entity.section.Section;

@Mapper
public interface SectionPortOutMapper {
    SectionPortOutMapper INSTANCE = Mappers.getMapper(SectionPortOutMapper.class);

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "orderIdx", source = "orderIdx")
    Section toDomain(SectionJpaEntity entity);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "project.id", source = "projectId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "orderIdx", source = "orderIdx")
    SectionJpaEntity toEntity(Section section);
}
