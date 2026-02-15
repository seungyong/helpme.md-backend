package seungyong.helpmebackend.adapter.out.persistence.mapper;

import javax.annotation.processing.Generated;
import seungyong.helpmebackend.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.adapter.out.persistence.entity.SectionJpaEntity;
import seungyong.helpmebackend.domain.entity.section.Section;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-08T09:45:12+0900",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.3.jar, environment: Java 17.0.3 (Oracle Corporation)"
)
public class SectionPortOutMapperImpl implements SectionPortOutMapper {

    @Override
    public Section toDomain(SectionJpaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Long projectId = null;
        String title = null;
        String content = null;
        Short orderIdx = null;
        Long id = null;

        projectId = entityProjectId( entity );
        title = entity.getTitle();
        content = entity.getContent();
        orderIdx = entity.getOrderIdx();
        id = entity.getId();

        Section section = new Section( id, projectId, title, content, orderIdx );

        return section;
    }

    @Override
    public SectionJpaEntity toEntity(Section section) {
        if ( section == null ) {
            return null;
        }

        SectionJpaEntity sectionJpaEntity = new SectionJpaEntity();

        sectionJpaEntity.setProject( sectionToProjectJpaEntity( section ) );
        sectionJpaEntity.setId( section.getId() );
        sectionJpaEntity.setTitle( section.getTitle() );
        sectionJpaEntity.setContent( section.getContent() );
        sectionJpaEntity.setOrderIdx( section.getOrderIdx() );

        return sectionJpaEntity;
    }

    private Long entityProjectId(SectionJpaEntity sectionJpaEntity) {
        if ( sectionJpaEntity == null ) {
            return null;
        }
        ProjectJpaEntity project = sectionJpaEntity.getProject();
        if ( project == null ) {
            return null;
        }
        Long id = project.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected ProjectJpaEntity sectionToProjectJpaEntity(Section section) {
        if ( section == null ) {
            return null;
        }

        ProjectJpaEntity projectJpaEntity = new ProjectJpaEntity();

        projectJpaEntity.setId( section.getProjectId() );

        return projectJpaEntity;
    }
}
