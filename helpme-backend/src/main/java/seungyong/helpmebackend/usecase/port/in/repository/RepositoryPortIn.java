package seungyong.helpmebackend.usecase.port.in.repository;

import seungyong.helpmebackend.adapter.in.web.dto.repository.response.ResponseRepositories;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.ResponseRepository;

public interface RepositoryPortIn {
    ResponseRepositories getRepositories(Long userId, Long installationId, Integer page);
    ResponseRepository getRepository(Long userId, String owner, String name);
}
