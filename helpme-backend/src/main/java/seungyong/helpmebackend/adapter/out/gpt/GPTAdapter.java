package seungyong.helpmebackend.adapter.out.gpt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.adapter.out.command.EvaluationCommand;
import seungyong.helpmebackend.adapter.out.command.RepositoryImportantCommand;
import seungyong.helpmebackend.adapter.out.command.RepositoryInfoCommand;
import seungyong.helpmebackend.adapter.out.result.RepositoryTreeResult;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.infrastructure.gpt.GPTClient;
import seungyong.helpmebackend.infrastructure.gpt.dto.EvaluationContent;
import seungyong.helpmebackend.infrastructure.gpt.dto.GPTRepositoryInfo;
import seungyong.helpmebackend.infrastructure.gpt.dto.ImportantFile;
import seungyong.helpmebackend.usecase.port.out.gpt.GPTPortOut;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GPTAdapter implements GPTPortOut {
    private final GPTClient gptClient;

    @Override
    public GPTRepositoryInfo getRepositoryInfo(String fullName, RepositoryInfoCommand command) {
        try {
            return gptClient.getRepositoryInfo(fullName, command);
        } catch (Exception e) {
            log.error("Error GPT repository info = {}", e.getMessage(), e);
            throw new CustomException(GlobalErrorCode.GPT_ERROR);
        }
    }

    @Override
    public List<RepositoryTreeResult> getImportantFiles(RepositoryImportantCommand command) {
        try {
            List<ImportantFile> importantFiles = gptClient.importantFiles(command);
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
    public EvaluationContent evaluateReadme(EvaluationCommand command) {
        try {
            return gptClient.evaluateReadme(command);
        } catch (Exception e) {
            log.error("Error GPT evaluate readme = {}", e.getMessage(), e);
            throw new CustomException(GlobalErrorCode.GPT_ERROR);
        }
    }
}
