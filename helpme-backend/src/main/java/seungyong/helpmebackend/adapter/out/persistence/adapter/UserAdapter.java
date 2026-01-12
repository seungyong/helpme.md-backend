package seungyong.helpmebackend.adapter.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import seungyong.helpmebackend.adapter.out.persistence.entity.UserJpaEntity;
import seungyong.helpmebackend.adapter.out.persistence.mapper.UserPortOutMapper;
import seungyong.helpmebackend.adapter.out.persistence.repository.UserJpaRepository;
import seungyong.helpmebackend.domain.entity.user.User;
import seungyong.helpmebackend.usecase.port.out.user.UserPortOut;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserAdapter implements UserPortOut {
    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(User user) {
        UserJpaEntity userJpaEntity = UserPortOutMapper.INSTANCE.toJpaEntity(user);
        UserJpaEntity savedEntity = userJpaRepository.save(userJpaEntity);
        return UserPortOutMapper.INSTANCE.toDomainEntity(savedEntity);
    }

    @Override
    public void delete(User user) {
        UserJpaEntity userJpaEntity = UserPortOutMapper.INSTANCE.toJpaEntity(user);
        userJpaRepository.delete(userJpaEntity);
    }

    @Override
    public Optional<User> getByGithubId(Long githubId) {
        Optional<UserJpaEntity> userJpaEntity = userJpaRepository.findByGithubId(githubId);
        return userJpaEntity.map(UserPortOutMapper.INSTANCE::toDomainEntity);
    }
}
