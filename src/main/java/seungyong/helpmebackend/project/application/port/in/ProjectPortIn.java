package seungyong.helpmebackend.project.application.port.in;

import seungyong.helpmebackend.project.application.port.in.command.CreateProjectCommand;
import seungyong.helpmebackend.project.application.port.in.command.UpdateProjectSettingsCommand;
import seungyong.helpmebackend.project.domain.entity.Project;

public interface ProjectPortIn {
    Project createProject(CreateProjectCommand command);

    Project retryInitialSync(Long userId, Long projectId);

    Project getProject(Long userId, Long projectId);

    Project getProjectSettings(Long userId, Long projectId);

    Project updateProjectSettings(UpdateProjectSettingsCommand command);
}
