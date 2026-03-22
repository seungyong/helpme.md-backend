package seungyong.helpmebackend.user.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import seungyong.helpmebackend.user.adapter.out.persistence.entity.UserJpaEntity;
import seungyong.helpmebackend.user.application.port.out.UserPortOutMapper;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.user.domain.exception.UserErrorCode;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;

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
    public User getById(Long id) {
        UserJpaEntity userJpaEntity = userJpaRepository.findById(id)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        return UserPortOutMapper.INSTANCE.toDomainEntity(userJpaEntity);
    }

    @Override
    public Optional<User> getByGithubId(Long githubId) {
        Optional<UserJpaEntity> userJpaEntity = userJpaRepository.findByGithubId(githubId);
        return userJpaEntity.map(UserPortOutMapper.INSTANCE::toDomainEntity);
    }
}
