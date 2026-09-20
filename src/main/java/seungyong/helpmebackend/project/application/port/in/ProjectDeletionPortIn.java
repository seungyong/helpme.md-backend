package seungyong.helpmebackend.project.application.port.in;

import seungyong.helpmebackend.project.application.port.in.command.DeleteProjectCommand;
import seungyong.helpmebackend.project.application.port.in.result.ProjectDeletionResult;

public interface ProjectDeletionPortIn {
    ProjectDeletionResult requestDeletion(DeleteProjectCommand command);

    void processNext();
}
