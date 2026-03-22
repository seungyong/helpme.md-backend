package seungyong.helpmebackend.project.application.port.out;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.project.domain.entity.Project;

@Mapper(componentModel = "spring")
public interface ProjectPortOutMapper {
    ProjectPortOutMapper INSTANCE = Mappers.getMapper(ProjectPortOutMapper.class);

    @Mapping(target = "user.id", source = "userId")
    @Mapping(target = "repoFullName", source = "repoFullName")
    ProjectJpaEntity toJpaEntity(Project project);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "repoFullName", source = "repoFullName")
    Project toDomain(ProjectJpaEntity entity);
}
