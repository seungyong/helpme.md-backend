package seungyong.helpmebackend.usecase.port.out.github.repository;

import seungyong.helpmebackend.adapter.out.result.RepositoryResult;

public interface RepositoryPortOut {
    RepositoryResult getRepositoriesByInstallationId(String accessToken, Long installationId, Integer page);
}
