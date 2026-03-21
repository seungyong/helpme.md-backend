package seungyong.helpmebackend.user.application.port.out;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import seungyong.helpmebackend.user.adapter.out.persistence.entity.UserJpaEntity;
import seungyong.helpmebackend.user.domain.entity.GithubUser;
import seungyong.helpmebackend.user.domain.entity.User;

@Mapper(componentModel = "spring")
public interface UserPortOutMapper {
    UserPortOutMapper INSTANCE = Mappers.getMapper(UserPortOutMapper.class);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "githubUser.name")
    @Mapping(target = "githubId", source = "githubUser.githubId")
    @Mapping(target = "githubToken", source = "githubUser.githubToken")
    UserJpaEntity toJpaEntity(User user);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "githubUser.name", source = "name")
    @Mapping(target = "githubUser.githubId", source = "githubId")
    @Mapping(target = "githubUser.githubToken", source = "githubToken")
    User toDomainEntity(UserJpaEntity userJpaEntity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "githubUser.name", source = "name")
    @Mapping(target = "githubUser.githubId", source = "githubId")
    @Mapping(target = "githubUser.githubToken", source = "githubToken")
    User toDomainEntity(GithubUser githubUser);
}
