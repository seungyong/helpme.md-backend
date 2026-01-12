package seungyong.helpmebackend.usecase.port.out.user;

import seungyong.helpmebackend.domain.entity.user.User;

import java.util.Optional;

public interface UserPortOut {
    User save(User user);
    void delete(User user);

    Optional<User> getByGithubId(Long githubId);
}
