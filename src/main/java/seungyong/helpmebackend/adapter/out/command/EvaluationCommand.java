package seungyong.helpmebackend.adapter.out.command;

import seungyong.helpmebackend.adapter.out.result.RepositoryFileContentResult;

import java.util.List;

public record EvaluationCommand(
        String fullName,
        String readmeContent,
        RepositoryInfoCommand repoInfo,
        List<RepositoryFileContentResult> entryPoints,
        List<RepositoryFileContentResult> importantFiles,
        String[] techStack,
        String projectSize
) {
}
