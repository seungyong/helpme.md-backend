package seungyong.helpmebackend.usecase.port.in.github;

import seungyong.helpmebackend.adapter.in.web.dto.repo.response.ResponseRepositories;

public interface GithubPortIn {
    ResponseRepositories getRepositories(Long userId);
}
