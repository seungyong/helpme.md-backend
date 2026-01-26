package seungyong.helpmebackend.adapter.in.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import seungyong.helpmebackend.adapter.in.web.dto.component.request.RequestComponent;
import seungyong.helpmebackend.adapter.in.web.dto.component.response.ResponseComponents;
import seungyong.helpmebackend.domain.entity.component.Component;

@Mapper
public interface ComponentPortInMapper {
    ComponentPortInMapper INSTANCE = Mappers.getMapper(ComponentPortInMapper.class);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "content", source = "content")
    ResponseComponents.Component toResponseComponent(seungyong.helpmebackend.domain.entity.component.Component component);

    @Mapping(target = "repoFullName", source = "fullName")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "title", source = "request.title")
    @Mapping(target = "content", source = "request.content")
    Component toDomain(RequestComponent request, String fullName, Long userId);
}
