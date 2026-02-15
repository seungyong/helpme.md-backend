package seungyong.helpmebackend.adapter.out.persistence.mapper;

import javax.annotation.processing.Generated;
import seungyong.helpmebackend.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.adapter.out.persistence.entity.UserJpaEntity;
import seungyong.helpmebackend.domain.entity.project.Project;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-08T09:45:12+0900",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.3.jar, environment: Java 17.0.3 (Oracle Corporation)"
)
public class ProjectPortOutMapperImpl implements ProjectPortOutMapper {

    @Override
    public ProjectJpaEntity toEntity(Project project) {
        if ( project == null ) {
            return null;
        }

        ProjectJpaEntity projectJpaEntity = new ProjectJpaEntity();

        projectJpaEntity.setUser( projectToUserJpaEntity( project ) );
        projectJpaEntity.setRepoFullName( project.getRepoFullName() );
        projectJpaEntity.setId( project.getId() );

        return projectJpaEntity;
    }

    @Override
    public Project toDomain(ProjectJpaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Long id = null;
        Long userId = null;
        String repoFullName = null;

        id = entity.getId();
        userId = entityUserId( entity );
        repoFullName = entity.getRepoFullName();

        Project project = new Project( id, userId, repoFullName );

        return project;
    }

    protected UserJpaEntity projectToUserJpaEntity(Project project) {
        if ( project == null ) {
            return null;
        }

        UserJpaEntity userJpaEntity = new UserJpaEntity();

        userJpaEntity.setId( project.getUserId() );

        return userJpaEntity;
    }

    private Long entityUserId(ProjectJpaEntity projectJpaEntity) {
        if ( projectJpaEntity == null ) {
            return null;
        }
        UserJpaEntity user = projectJpaEntity.getUser();
        if ( user == null ) {
            return null;
        }
        Long id = user.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
