package seungyong.helpmebackend.usecase.port.in.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import seungyong.helpmebackend.adapter.in.web.dto.repository.request.RequestDraftEvaluation;
import seungyong.helpmebackend.adapter.in.web.dto.repository.request.RequestEvaluation;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.ResponseDraftReadme;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.ResponseEvaluation;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.ResponseRepositories;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.ResponseRepository;

public interface RepositoryPortIn {
    ResponseRepositories getRepositories(Long userId, Long installationId, Integer page);
    ResponseRepository getRepository(Long userId, String owner, String name);
    ResponseEvaluation evaluateReadme(RequestEvaluation request, Long userId, String owner, String name) throws JsonProcessingException;
    ResponseEvaluation evaluateDraftReadme(RequestDraftEvaluation request, Long userId, String owner, String name) throws JsonProcessingException;
    ResponseDraftReadme generateDraftReadme(RequestEvaluation request, Long userId, String owner, String name) throws JsonProcessingException;
}
