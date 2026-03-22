package seungyong.helpmebackend.repository.application.port.in;

import seungyong.helpmebackend.repository.adapter.in.web.dto.request.RequestDraftEvaluation;
import seungyong.helpmebackend.repository.adapter.in.web.dto.request.RequestGeneration;
import seungyong.helpmebackend.repository.adapter.in.web.dto.request.RequestPull;
import seungyong.helpmebackend.repository.adapter.in.web.dto.response.*;
import seungyong.helpmebackend.section.adapter.in.web.dto.response.ResponseSections;

public interface RepositoryPortIn {
    ResponseRepositories getRepositories(Long userId, Long installationId, Integer page, Integer perPage);
    ResponseRepository getRepository(Long userId, String owner, String name);
    ResponseBranches getBranches(Long userId, String owner, String name);
    ResponseEvaluation fallbackDraftEvaluation(String taskId);
    ResponseSections fallbackGenerateReadme(String taskId);
    ResponsePull createPullRequest(RequestPull request, Long userId, String owner, String name);
    void evaluateDraftReadme(RequestDraftEvaluation request, String taskId, Long userId, String owner, String name);
    void generateDraftReadme(RequestGeneration request, String taskId, Long userId, String owner, String name);
}
