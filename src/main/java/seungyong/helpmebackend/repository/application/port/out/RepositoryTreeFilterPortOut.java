package seungyong.helpmebackend.repository.application.port.out;

import seungyong.helpmebackend.repository.application.port.out.result.RepositoryTreeResult;

import java.util.List;

public interface RepositoryTreeFilterPortOut {
    List<RepositoryTreeResult> filter(List<RepositoryTreeResult> tree);
}
