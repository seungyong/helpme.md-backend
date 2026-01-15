package seungyong.helpmebackend.usecase.port.in.repository;

import seungyong.helpmebackend.adapter.in.web.dto.repository.response.ResponseRepositories;

public interface RepositoryPortIn {
    ResponseRepositories getRepositories(Long userId, Long installationId, Integer page);
}
