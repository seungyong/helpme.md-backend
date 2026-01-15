package seungyong.helpmebackend.adapter.out.repository;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.adapter.out.result.RepositoryResult;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.domain.entity.Repository;
import seungyong.helpmebackend.infrastructure.github.GithubAPI;
import seungyong.helpmebackend.usecase.port.out.github.GithubPortConfig;
import seungyong.helpmebackend.usecase.port.out.github.repository.RepositoryPortOut;

import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class RepositoryAdapter extends GithubPortConfig implements RepositoryPortOut {
    private final GithubAPI githubAPI;

    @Override
    public RepositoryResult getRepositoriesByInstallationId(String accessToken, Long installationId, Integer page) {
        String url = String.format("https://api.github.com/user/installations/%d/repositories?per_page=30&page=%d", installationId, page);
        String responseBody = githubAPI.fetchGetMethod(url, accessToken);

        try {
            JsonNode jsonNode = super.getObjectMapper().readTree(responseBody);
            ArrayList<Repository> repositories = new ArrayList<>();

            for (JsonNode repo : jsonNode.get("repositories")) {
                String avatarUrl = repo.get("owner").get("avatar_url").asText();
                String name = repo.get("name").asText();
                String owner = repo.get("owner").get("login").asText();

                repositories.add(new Repository(avatarUrl, name, owner));
            }

            return new RepositoryResult(repositories, jsonNode.get("total_count").asInt());
        } catch (Exception e) {
            log.error("Error parsing Github intsallations response = {}", responseBody, e);
            throw new CustomException(GlobalErrorCode.GITHUB_ERROR);
        }
    }
}
