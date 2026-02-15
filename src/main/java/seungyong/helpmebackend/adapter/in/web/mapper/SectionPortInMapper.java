package seungyong.helpmebackend.adapter.in.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.ResponseRepository;
import seungyong.helpmebackend.adapter.in.web.dto.section.response.ResponseSections;
import seungyong.helpmebackend.domain.entity.section.Section;

@Mapper
public interface SectionPortInMapper {
    SectionPortInMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(SectionPortInMapper.class);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "orderIdx", source = "orderIdx")
    ResponseSections.Section toResponseSection(Section section);
}
