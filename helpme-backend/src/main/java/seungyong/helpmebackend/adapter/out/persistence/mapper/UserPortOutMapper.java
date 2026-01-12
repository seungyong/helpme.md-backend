package seungyong.helpmebackend.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import seungyong.helpmebackend.adapter.out.persistence.entity.UserJpaEntity;
import seungyong.helpmebackend.domain.entity.user.User;

@Mapper
public interface UserPortOutMapper {
    UserPortOutMapper INSTANCE = Mappers.getMapper(UserPortOutMapper.class);

    @Mapping(target = "githubId", source = "githubId")
    @Mapping(target = "githubToken", source = "githubToken")
    UserJpaEntity toJpaEntity(User user);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "githubId", source = "githubId")
    @Mapping(target = "githubToken", source = "githubToken")
    User toDomainEntity(UserJpaEntity userJpaEntity);

    @Mapping(target = "githubId", source = "githubId")
    @Mapping(target = "githubToken", source = "githubToken")
    User toDomainEntity(Long githubId, String githubToken);
}
