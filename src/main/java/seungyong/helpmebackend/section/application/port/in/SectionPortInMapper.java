package seungyong.helpmebackend.section.application.port.in;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import seungyong.helpmebackend.section.adapter.in.web.dto.response.ResponseSections;
import seungyong.helpmebackend.section.domain.entity.Section;

@Mapper
public interface SectionPortInMapper {
    SectionPortInMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(SectionPortInMapper.class);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "orderIdx", source = "orderIdx")
    ResponseSections.Section toResponseSection(Section section);
}
