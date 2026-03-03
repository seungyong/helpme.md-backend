package seungyong.helpmebackend.repository.application.port.out;

import seungyong.helpmebackend.repository.application.port.out.command.EvaluationCommand;
import seungyong.helpmebackend.repository.application.port.out.command.GenerateReadmeCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepositoryInfoCommand;
import seungyong.helpmebackend.repository.application.port.out.result.EvaluationContentResult;
import seungyong.helpmebackend.repository.application.port.out.result.GPTRepositoryInfoResult;

public interface GPTPortOut {
    GPTRepositoryInfoResult getRepositoryInfo(String fullName, RepositoryInfoCommand command);

    EvaluationContentResult evaluateReadme(EvaluationCommand command);
    String generateDraftReadme(GenerateReadmeCommand command);
}
