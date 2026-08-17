package seungyong.helpmebackend.project.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import seungyong.helpmebackend.github.application.port.in.GithubRepositoryAccessPortIn;
import seungyong.helpmebackend.github.domain.entity.GithubRepository;
import seungyong.helpmebackend.github.domain.exception.GithubErrorCode;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.project.application.port.in.ProjectPortIn;
import seungyong.helpmebackend.project.application.port.in.command.CreateProjectCommand;
import seungyong.helpmebackend.project.application.port.in.command.UpdateProjectSettingsCommand;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.entity.ProjectSettings;
import seungyong.helpmebackend.project.domain.exception.ProjectErrorCode;
import seungyong.helpmebackend.project.domain.type.ProjectSyncStatus;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.webhook.application.port.out.WebhookWorkPortOut;

import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectService implements ProjectPortIn {
    private final ProjectAccessResolver projectAccessResolver;
    private final ProjectPortOut projectPortOut;
    private final GithubRepositoryAccessPortIn githubRepositoryAccessPortIn;
    private final UserPortOut userPortOut;
    private final ProjectCreationWriter projectCreationWriter;
    private final WebhookWorkPortOut webhookWorkPortOut;

    @Override
    public Project createProject(CreateProjectCommand command) {
        validateCreateCommand(command);
        if (projectPortOut.getByUserIdAndGithubRepoId(
                command.userId(), command.githubRepoId()).isPresent()) {
            throw new CustomException(ProjectErrorCode.PROJECT_ALREADY_CONNECTED);
        }
        User user = userPortOut.getById(command.userId());
        if (projectPortOut.countByUserId(command.userId()) >= user.getPlan().projectLimit()) {
            throw new CustomException(ProjectErrorCode.PROJECT_LIMIT_EXCEEDED);
        }

        GithubRepository repository = githubRepositoryAccessPortIn.getRepository(
                command.userId(), command.githubInstallationId(), command.githubRepoId()
        );
        if (!repository.permissions().push()) {
            throw new CustomException(GithubErrorCode.GITHUB_PERMISSION_DENIED);
        }
        LinkedHashSet<String> requiredBranches = new LinkedHashSet<>();
        requiredBranches.add(command.defaultBranch());
        if (!command.trackAllBranches()) {
            requiredBranches.addAll(command.trackedBranches());
        }
        githubRepositoryAccessPortIn.validateRepositoryBranches(
                command.userId(), command.githubInstallationId(), command.githubRepoId(),
                repository.fullName(), requiredBranches
        );

        ProjectSettings defaults = ProjectSettings.defaults();
        ProjectSettings settings;
        try {
            settings = new ProjectSettings(
                    command.trackedBranches(), command.trackAllBranches(), command.timezone(),
                    defaults.daily(), defaults.weekly(), defaults.webhookPayloadRetentionDays()
            );
        } catch (IllegalArgumentException exception) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
        Project project = Project.builder()
                .userId(command.userId())
                .repoFullName(repository.fullName())
                .githubRepoId(repository.githubRepoId())
                .githubInstallationId(command.githubInstallationId())
                .defaultBranch(command.defaultBranch())
                .privateRepository(repository.privateRepository())
                .settings(settings)
                .build();
        return projectCreationWriter.create(project);
    }

    @Override
    public Project retryInitialSync(Long userId, Long projectId) {
        Project project = projectAccessResolver.resolveActive(userId, projectId);
        if (project.getSync().status() == ProjectSyncStatus.READY) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
        if (project.getSync().status() == ProjectSyncStatus.FAILED) {
            webhookWorkPortOut.retryInitialSync(projectId);
        }
        return projectPortOut.getById(projectId).orElse(project);
    }

    @Override
    @Transactional(readOnly = true)
    public Project getProject(Long userId, Long projectId) {
        return projectAccessResolver.resolveActive(userId, projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public Project getProjectSettings(Long userId, Long projectId) {
        return projectAccessResolver.resolveActive(userId, projectId);
    }

    @Override
    public Project updateProjectSettings(UpdateProjectSettingsCommand command) {
        Project project = projectAccessResolver.resolveActive(command.userId(), command.projectId());
        if (!command.hasChanges()) {
            return project;
        }

        ProjectSettings currentSettings = project.getSettings();
        ProjectSettings changedSettings = mergeSettings(currentSettings, command);
        if (changedSettings.equals(currentSettings)) {
            return project;
        }

        validateBranches(project, currentSettings, changedSettings);
        project.changeSettings(changedSettings);
        return projectPortOut.updateSettings(project.getId(), changedSettings);
    }

    private ProjectSettings mergeSettings(
            ProjectSettings current,
            UpdateProjectSettingsCommand command
    ) {
        validateTimezone(command.timezone());
        try {
            return new ProjectSettings(
                    valueOrCurrent(command.trackedBranches(), current.trackedBranches()),
                    valueOrCurrent(command.trackAllBranches(), current.trackAllBranches()),
                    valueOrCurrent(command.timezone(), current.timezone()),
                    new ProjectSettings.DailyReflectionSchedule(
                            valueOrCurrent(command.dailyEnabled(), current.daily().enabled()),
                            valueOrCurrent(command.dailyGenerationTime(), current.daily().generationTime())
                    ),
                    new ProjectSettings.WeeklyReflectionSchedule(
                            valueOrCurrent(command.weeklyEnabled(), current.weekly().enabled()),
                            valueOrCurrent(command.weeklyGenerationDay(), current.weekly().generationDay()),
                            valueOrCurrent(command.weeklyGenerationTime(), current.weekly().generationTime())
                    ),
                    valueOrCurrent(
                            command.webhookPayloadRetentionDays(),
                            current.webhookPayloadRetentionDays()
                    )
            );
        } catch (IllegalArgumentException exception) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private void validateTimezone(String timezone) {
        if (timezone == null) {
            return;
        }
        if (!StringUtils.hasText(timezone) || !ZoneId.getAvailableZoneIds().contains(timezone)) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private void validateBranches(
            Project project,
            ProjectSettings currentSettings,
            ProjectSettings changedSettings
    ) {
        if (hasSameBranchConfiguration(currentSettings, changedSettings)
                || changedSettings.trackAllBranches()) {
            return;
        }

        Set<String> trackedBranches = Set.copyOf(changedSettings.trackedBranches());
        if (trackedBranches.isEmpty()) {
            return;
        }

        githubRepositoryAccessPortIn.validateRepositoryBranches(
                project.getUserId(),
                project.getGithubInstallationId(),
                project.getGithubRepoId(),
                project.getRepoFullName(),
                trackedBranches
        );
    }

    private boolean hasSameBranchConfiguration(
            ProjectSettings currentSettings,
            ProjectSettings changedSettings
    ) {
        return currentSettings.trackAllBranches() == changedSettings.trackAllBranches()
                && Set.copyOf(currentSettings.trackedBranches())
                .equals(Set.copyOf(changedSettings.trackedBranches()));
    }

    private <T> T valueOrCurrent(T changed, T current) {
        return changed == null ? current : changed;
    }

    private void validateCreateCommand(CreateProjectCommand command) {
        if (command == null || command.userId() == null
                || command.githubInstallationId() == null || command.githubInstallationId() < 1
                || command.githubRepoId() == null || command.githubRepoId() < 1
                || !StringUtils.hasText(command.defaultBranch())
                || !StringUtils.hasText(command.timezone())
                || !ZoneId.getAvailableZoneIds().contains(command.timezone())) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private boolean valueOrCurrent(Boolean changed, boolean current) {
        return changed == null ? current : changed;
    }

    private short valueOrCurrent(Short changed, short current) {
        return changed == null ? current : changed;
    }
}
