package seungyong.helpmebackend.user.application.port.out;

import seungyong.helpmebackend.user.domain.entity.User;

import java.util.Optional;

public interface UserPortOut {
    User save(User user);
    void delete(User user);

    User getById(Long id);
    Optional<User> getByGithubId(Long githubId);
}
