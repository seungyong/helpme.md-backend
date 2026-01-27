package seungyong.helpmebackend.usecase.port.out.github.repository;

import seungyong.helpmebackend.adapter.out.result.RepositoryTreeResult;

import java.util.List;

public interface RepositoryTreeFilterPortOut {
    List<RepositoryTreeResult> filter(List<RepositoryTreeResult> tree);
}
