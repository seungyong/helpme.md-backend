package seungyong.helpmebackend.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import seungyong.helpmebackend.adapter.out.persistence.entity.UserJpaEntity;
import seungyong.helpmebackend.domain.entity.user.GithubUser;
import seungyong.helpmebackend.domain.entity.user.User;

@Mapper
public interface UserPortOutMapper {
    UserPortOutMapper INSTANCE = Mappers.getMapper(UserPortOutMapper.class);

    @Mapping(target = "name", source = "githubUser.name")
    @Mapping(target = "githubId", source = "githubUser.githubId")
    @Mapping(target = "githubToken", source = "githubUser.githubToken")
    UserJpaEntity toJpaEntity(User user);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "githubUser.name", source = "name")
    @Mapping(target = "githubUser.githubId", source = "githubId")
    @Mapping(target = "githubUser.githubToken", source = "githubToken")
    User toDomainEntity(UserJpaEntity userJpaEntity);

    @Mapping(target = "githubUser.name", source = "name")
    @Mapping(target = "githubUser.githubId", source = "githubId")
    @Mapping(target = "githubUser.githubToken", source = "githubToken")
    User toDomainEntity(GithubUser githubUser);
}
