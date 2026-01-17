package seungyong.helpmebackend.adapter.out.gpt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.adapter.out.result.RepositoryFileContentResult;
import seungyong.helpmebackend.adapter.out.result.RepositoryTreeResult;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.infrastructure.gpt.EvaluationContent;
import seungyong.helpmebackend.infrastructure.gpt.GPTClient;
import seungyong.helpmebackend.infrastructure.gpt.ImportantFile;
import seungyong.helpmebackend.usecase.port.out.gpt.GPTPortOut;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GPTAdapter implements GPTPortOut {
    private final GPTClient gptClient;

    @Override
    public List<RepositoryTreeResult> getImportantFiles(List<RepositoryTreeResult> trees) {
        try {
            List<ImportantFile> importantFiles = gptClient.importantFiles(trees);
            return importantFiles.stream()
                    .map(file -> new RepositoryTreeResult(
                            file.path(),
                            "file"
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Error GPT important files = {}", e.getMessage(), e);
            throw new CustomException(GlobalErrorCode.GPT_ERROR);
        }
    }

    @Override
    public EvaluationContent evaluateReadme(String readmeContent, List<String> commits, List<RepositoryTreeResult> trees, List<RepositoryFileContentResult> importantFiles) {
        try {
            return gptClient.evaluateReadme(readmeContent, commits, trees, importantFiles);
        } catch (Exception e) {
            log.error("Error GPT evaluate readme = {}", e.getMessage(), e);
            throw new CustomException(GlobalErrorCode.GPT_ERROR);
        }
    }
}
