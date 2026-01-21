package seungyong.helpmebackend.usecase.service.github.dto;

import seungyong.helpmebackend.adapter.out.command.RepositoryInfoCommand;
import seungyong.helpmebackend.adapter.out.result.RepositoryFileContentResult;
import seungyong.helpmebackend.adapter.out.result.RepositoryLanguageResult;
import seungyong.helpmebackend.adapter.out.result.RepositoryTreeResult;
import seungyong.helpmebackend.infrastructure.gpt.dto.GPTRepositoryInfo;

import java.util.List;

public record ReadmeContext(
        RepositoryInfoCommand.CommitCommand commits,
        GPTRepositoryInfo repositoryInfo,
        List<RepositoryLanguageResult> languages,
        List<RepositoryTreeResult> trees,
        List<RepositoryFileContentResult> entryContents,
        List<RepositoryFileContentResult> importantFileContents
) {
}
