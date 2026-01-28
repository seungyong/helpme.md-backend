package seungyong.helpmebackend.usecase.port.out.gpt;

import seungyong.helpmebackend.adapter.out.command.EvaluationCommand;
import seungyong.helpmebackend.adapter.out.command.GenerateReadmeCommand;
import seungyong.helpmebackend.adapter.out.command.RepositoryImportantCommand;
import seungyong.helpmebackend.adapter.out.command.RepositoryInfoCommand;
import seungyong.helpmebackend.adapter.out.result.GPTRepositoryInfoResult;
import seungyong.helpmebackend.adapter.out.result.RepositoryTreeResult;
import seungyong.helpmebackend.adapter.out.result.EvaluationContentResult;

import java.util.List;

public interface GPTPortOut {
    GPTRepositoryInfoResult getRepositoryInfo(String fullName, RepositoryInfoCommand command);

    EvaluationContentResult evaluateReadme(EvaluationCommand command);
    String generateDraftReadme(GenerateReadmeCommand command);
}
