package seungyong.helpmebackend.usecase.port.in.repository;

import seungyong.helpmebackend.adapter.in.web.dto.repository.request.RequestDraftEvaluation;
import seungyong.helpmebackend.adapter.in.web.dto.repository.request.RequestEvaluation;
import seungyong.helpmebackend.adapter.in.web.dto.repository.request.RequestPull;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.*;

public interface RepositoryPortIn {
    ResponseRepositories getRepositories(Long userId, Long installationId, Integer page, Integer perPage);
    ResponseRepository getRepository(Long userId, String owner, String name);
    ResponseBranches getBranches(Long userId, String owner, String name);
    ResponseEvaluation fallbackDraftEvaluation(String taskId);
    ResponseDraftReadme fallbackGenerateReadme(String taskId);
    ResponsePull createPullRequest(RequestPull request, Long userId, String owner, String name);
    void evaluateDraftReadme(RequestDraftEvaluation request, String taskId, Long userId, String owner, String name);
    void generateDraftReadme(RequestEvaluation request, String taskId, Long userId, String owner, String name);
}
