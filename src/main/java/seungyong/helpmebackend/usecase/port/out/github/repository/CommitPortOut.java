package seungyong.helpmebackend.usecase.port.out.github.repository;

import seungyong.helpmebackend.adapter.out.command.RepoBranchCommand;
import seungyong.helpmebackend.adapter.out.result.CommitResult;
import seungyong.helpmebackend.adapter.out.result.ContributorsResult;

public interface CommitPortOut {
    CommitResult getCommits(RepoBranchCommand command, ContributorsResult.Contributor contributor);
}
