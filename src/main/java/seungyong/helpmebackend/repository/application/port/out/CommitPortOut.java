package seungyong.helpmebackend.repository.application.port.out;

import seungyong.helpmebackend.repository.application.port.out.command.RepoBranchCommand;
import seungyong.helpmebackend.repository.application.port.out.result.CommitResult;
import seungyong.helpmebackend.repository.application.port.out.result.ContributorsResult;

public interface CommitPortOut {
    CommitResult getCommits(RepoBranchCommand command, ContributorsResult.Contributor contributor);
}
