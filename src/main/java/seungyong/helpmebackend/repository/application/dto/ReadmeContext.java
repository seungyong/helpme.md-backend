package seungyong.helpmebackend.repository.application.dto;

import seungyong.helpmebackend.repository.application.port.out.command.RepositoryInfoCommand;
import seungyong.helpmebackend.repository.application.port.out.result.GPTRepositoryInfoResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryFileContentResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryLanguageResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryTreeResult;

import java.util.List;

public record ReadmeContext(
        String readme,
        List<RepositoryInfoCommand.CommitCommand> commits,
        GPTRepositoryInfoResult repositoryInfo,
        List<RepositoryLanguageResult> languages,
        List<RepositoryTreeResult> trees,
        List<RepositoryFileContentResult> entryContents,
        List<RepositoryFileContentResult> importantFileContents
) {
}
