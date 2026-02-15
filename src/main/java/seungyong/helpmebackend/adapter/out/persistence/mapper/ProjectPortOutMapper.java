package seungyong.helpmebackend.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import seungyong.helpmebackend.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.domain.entity.project.Project;

@Mapper
public interface ProjectPortOutMapper {
    ProjectPortOutMapper INSTANCE = Mappers.getMapper(ProjectPortOutMapper.class);

    @Mapping(target = "user.id", source = "userId")
    @Mapping(target = "repoFullName", source = "repoFullName")
    ProjectJpaEntity toEntity(Project project);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "repoFullName", source = "repoFullName")
    Project toDomain(ProjectJpaEntity entity);
}
