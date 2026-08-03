package seungyong.helpmebackend.github.application.port.out;

import seungyong.helpmebackend.github.domain.entity.GithubInstallation;
import seungyong.helpmebackend.github.domain.entity.GithubRepositoryPage;

import java.util.List;

public interface GithubAppPortOut {
    List<GithubInstallation> getInstallations(Long userId, String accessToken);

    GithubRepositoryPage getRepositories(
            Long userId,
            String accessToken,
            Long installationId,
            String query,
            int page,
            int size
    );
}
