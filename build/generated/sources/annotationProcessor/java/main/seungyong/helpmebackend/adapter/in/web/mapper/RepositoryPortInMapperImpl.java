package seungyong.helpmebackend.adapter.in.web.mapper;

import javax.annotation.processing.Generated;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.ResponseRepository;
import seungyong.helpmebackend.adapter.out.result.RepositoryDetailResult;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-08T09:45:12+0900",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.3.jar, environment: Java 17.0.3 (Oracle Corporation)"
)
public class RepositoryPortInMapperImpl implements RepositoryPortInMapper {

    @Override
    public ResponseRepository toResponseRepository(RepositoryDetailResult result) {
        if ( result == null ) {
            return null;
        }

        String owner = null;
        String name = null;
        String avatarUrl = null;
        String defaultBranch = null;

        owner = result.owner();
        name = result.name();
        avatarUrl = result.avatarUrl();
        defaultBranch = result.defaultBranch();

        ResponseRepository responseRepository = new ResponseRepository( owner, name, avatarUrl, defaultBranch );

        return responseRepository;
    }
}
