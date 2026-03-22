package seungyong.helpmebackend.repository.application.port.out.command;

import seungyong.helpmebackend.repository.application.port.out.result.RepositoryFileContentResult;

import java.util.List;

public record GenerateReadmeCommand(
        String fullName,
        String readme,
        RepositoryInfoCommand repoInfo,
        List<RepositoryFileContentResult> entryPoints,
        List<RepositoryFileContentResult> importantFiles,
        String[] techStack,
        String projectSize
) {
}
