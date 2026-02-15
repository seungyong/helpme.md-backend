package seungyong.helpmebackend.adapter.in.web.mapper;

import javax.annotation.processing.Generated;
import seungyong.helpmebackend.adapter.in.web.dto.section.response.ResponseSections;
import seungyong.helpmebackend.domain.entity.section.Section;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-08T09:45:12+0900",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.3.jar, environment: Java 17.0.3 (Oracle Corporation)"
)
public class SectionPortInMapperImpl implements SectionPortInMapper {

    @Override
    public ResponseSections.Section toResponseSection(Section section) {
        if ( section == null ) {
            return null;
        }

        Long id = null;
        String title = null;
        String content = null;
        Short orderIdx = null;

        id = section.getId();
        title = section.getTitle();
        content = section.getContent();
        orderIdx = section.getOrderIdx();

        ResponseSections.Section section1 = new ResponseSections.Section( id, title, content, orderIdx );

        return section1;
    }
}
