package seungyong.helpmebackend.usecase.port.out.gpt;

import seungyong.helpmebackend.adapter.out.command.EvaluationCommand;
import seungyong.helpmebackend.adapter.out.command.RepositoryImportantCommand;
import seungyong.helpmebackend.adapter.out.command.RepositoryInfoCommand;
import seungyong.helpmebackend.adapter.out.result.*;
import seungyong.helpmebackend.infrastructure.gpt.dto.EvaluationContent;
import seungyong.helpmebackend.infrastructure.gpt.dto.GPTRepositoryInfo;

import java.util.List;

public interface GPTPortOut {
    GPTRepositoryInfo getRepositoryInfo(String fullName, RepositoryInfoCommand command);
    List<RepositoryTreeResult> getImportantFiles(RepositoryImportantCommand command);
    EvaluationContent evaluateReadme(EvaluationCommand command);
}
