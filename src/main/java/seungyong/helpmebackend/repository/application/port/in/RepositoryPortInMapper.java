package seungyong.helpmebackend.repository.application.port.in;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import seungyong.helpmebackend.repository.adapter.in.web.dto.response.ResponseRepository;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryDetailResult;

@Mapper
public interface RepositoryPortInMapper {
    RepositoryPortInMapper INSTANCE = Mappers.getMapper(RepositoryPortInMapper.class);

    @Mapping(target = "owner", source = "result.owner")
    @Mapping(target = "name", source = "result.name")
    @Mapping(target = "avatarUrl", source = "result.avatarUrl")
    @Mapping(target = "defaultBranch", source = "result.defaultBranch")
    ResponseRepository toResponseRepository(RepositoryDetailResult result);
}
