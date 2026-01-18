package seungyong.helpmebackend.usecase.port.out.github.repository;

import seungyong.helpmebackend.adapter.out.result.RepositoryFileContentResult;
import seungyong.helpmebackend.adapter.out.result.RepositoryResult;
import seungyong.helpmebackend.adapter.out.result.RepositoryDetailResult;
import seungyong.helpmebackend.adapter.out.result.RepositoryTreeResult;

import java.util.List;

public interface RepositoryPortOut {
    RepositoryResult getRepositoriesByInstallationId(String accessToken, Long installationId, Integer page);
    RepositoryDetailResult getRepository(String accessToken, String owner, String name);
    String getReadmeContent(String accessToken, String owner, String name, String branch);
    List<String> getAllBranches(String accessToken, String owner, String name);
    List<String> getCommitsByBranch(String accessToken, String owner, String name, String branch);
    List<RepositoryTreeResult> getRepositoryTree(String accessToken, String owner, String name, String branch);
    RepositoryFileContentResult getFileContent(String accessToken, String owner, String name, RepositoryTreeResult file);
}
