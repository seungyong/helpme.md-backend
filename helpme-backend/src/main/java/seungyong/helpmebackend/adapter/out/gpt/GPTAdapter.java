package seungyong.helpmebackend.adapter.out.gpt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.adapter.out.command.EvaluationCommand;
import seungyong.helpmebackend.adapter.out.command.GenerateReadmeCommand;
import seungyong.helpmebackend.adapter.out.command.RepositoryInfoCommand;
import seungyong.helpmebackend.adapter.out.result.EvaluationContentResult;
import seungyong.helpmebackend.adapter.out.result.GPTRepositoryInfoResult;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.infrastructure.gpt.GPTClient;
import seungyong.helpmebackend.usecase.port.out.gpt.GPTPortOut;

@Slf4j
@Component
@RequiredArgsConstructor
public class GPTAdapter implements GPTPortOut {
    private final GPTClient gptClient;

    @Override
    public GPTRepositoryInfoResult getRepositoryInfo(String fullName, RepositoryInfoCommand command) {
        try {
            return gptClient.getRepositoryInfo(fullName, command);
        } catch (Exception e) {
            log.error("Error GPT repository info = {}", e.getMessage(), e);
            throw new CustomException(GlobalErrorCode.GPT_ERROR);
        }
    }

    @Override
    public EvaluationContentResult evaluateReadme(EvaluationCommand command) {
        try {
            return gptClient.evaluateReadme(command);
        } catch (Exception e) {
            log.error("Error GPT evaluate readme = {}", e.getMessage(), e);
            throw new CustomException(GlobalErrorCode.GPT_ERROR);
        }
    }

    @Override
    public String generateDraftReadme(GenerateReadmeCommand command) {
        try {
            return gptClient.generateDraftReadme(command);
        } catch (Exception e) {
            log.error("Error GPT generate draft readme = {}", e.getMessage(), e);
            throw new CustomException(GlobalErrorCode.GPT_ERROR);
        }
    }
}
