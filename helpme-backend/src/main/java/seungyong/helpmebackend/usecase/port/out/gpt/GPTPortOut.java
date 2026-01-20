package seungyong.helpmebackend.usecase.port.out.gpt;

import seungyong.helpmebackend.adapter.out.result.RepositoryFileContentResult;
import seungyong.helpmebackend.adapter.out.result.RepositoryTreeResult;
import seungyong.helpmebackend.infrastructure.gpt.dto.EvaluationContent;

import java.util.List;

public interface GPTPortOut {
    List<RepositoryTreeResult> getImportantFiles(List<RepositoryTreeResult> trees);
    EvaluationContent evaluateReadme(
            String readmeContent,
            List<String> commits,
            List<RepositoryTreeResult> trees,
            List<RepositoryFileContentResult> importantFiles
    );
}
