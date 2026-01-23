package seungyong.helpmebackend.usecase.port.in.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import seungyong.helpmebackend.adapter.in.web.dto.repository.request.RequestDraftEvaluation;
import seungyong.helpmebackend.adapter.in.web.dto.repository.request.RequestEvaluation;
import seungyong.helpmebackend.adapter.in.web.dto.repository.request.RequestPull;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.*;

public interface RepositoryPortIn {
    ResponseRepositories getRepositories(Long userId, Long installationId, Integer page);
    ResponseRepository getRepository(Long userId, String owner, String name);
    ResponsePull createPullRequest(RequestPull request, Long userId, String owner, String name);
    ResponseEvaluation evaluateReadme(RequestEvaluation request, Long userId, String owner, String name) throws JsonProcessingException;
    ResponseEvaluation evaluateDraftReadme(RequestDraftEvaluation request, Long userId, String owner, String name) throws JsonProcessingException;
    ResponseDraftReadme generateDraftReadme(RequestEvaluation request, Long userId, String owner, String name) throws JsonProcessingException;
}
