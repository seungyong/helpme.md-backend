package seungyong.helpmebackend.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import seungyong.helpmebackend.adapter.out.persistence.entity.ComponentJpaEntity;
import seungyong.helpmebackend.domain.entity.component.Component;

@Mapper
public interface ComponentPortOutMapper {
    ComponentPortOutMapper INSTANCE = Mappers.getMapper(ComponentPortOutMapper.class);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "repoFullName", source = "repoFullName")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "updatedAt", source = "updatedAt")
    Component toDomain(ComponentJpaEntity entity);
}
