package seungyong.helpmebackend.repository.application.port.out;

import seungyong.helpmebackend.repository.application.port.out.command.RepoBranchCommand;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryFileContentResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryTreeResult;

import java.util.List;

public interface RepositoryContentPortOut {
    String getRecentSHA(RepoBranchCommand command);
    String getReadmeSHA(RepoBranchCommand command);
    String getReadmeContent(RepoBranchCommand command);
    List<RepositoryTreeResult> getRepositoryTree(RepoBranchCommand command);
    RepositoryFileContentResult getFileContent(RepoBranchCommand command, RepositoryTreeResult file);
}
