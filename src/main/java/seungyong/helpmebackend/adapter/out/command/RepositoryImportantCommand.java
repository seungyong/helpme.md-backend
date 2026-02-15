package seungyong.helpmebackend.adapter.out.command;

import seungyong.helpmebackend.adapter.out.result.RepositoryFileContentResult;

import java.util.List;

public record RepositoryImportantCommand(
        String fullName,
        RepositoryInfoCommand repoInfo,
        List<RepositoryFileContentResult> entryPoints,
        String[] techStack,
        String projectSize
) {
}
