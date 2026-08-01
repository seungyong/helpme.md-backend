package seungyong.helpmebackend.repository.application.port.out;

import seungyong.helpmebackend.repository.application.port.out.command.CreateBranchCommand;
import seungyong.helpmebackend.repository.application.port.out.command.CreatePullRequestCommand;
import seungyong.helpmebackend.repository.application.port.out.command.ReadmePushCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepoBranchCommand;

public interface RepositoryMutationPortOut {
    void createBranch(CreateBranchCommand command);
    void deleteBranch(RepoBranchCommand command);
    void push(ReadmePushCommand command);
    String createPullRequest(CreatePullRequestCommand command);
}
