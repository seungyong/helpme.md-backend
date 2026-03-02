package seungyong.helpmebackend.adapter.in.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.ResponseRepository;
import seungyong.helpmebackend.adapter.out.result.RepositoryDetailResult;

@Mapper
public interface RepositoryPortInMapper {
    RepositoryPortInMapper INSTANCE = Mappers.getMapper(RepositoryPortInMapper.class);

    @Mapping(target = "owner", source = "result.owner")
    @Mapping(target = "name", source = "result.name")
    @Mapping(target = "avatarUrl", source = "result.avatarUrl")
    @Mapping(target = "defaultBranch", source = "result.defaultBranch")
    ResponseRepository toResponseRepository(RepositoryDetailResult result);
}
