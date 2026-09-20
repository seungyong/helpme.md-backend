package seungyong.helpmebackend.project.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;

import seungyong.helpmebackend.project.application.port.in.ProjectDeletionPortIn;
import seungyong.helpmebackend.project.application.port.in.command.DeleteProjectCommand;
import seungyong.helpmebackend.project.application.port.in.result.ProjectDeletionResult;
import seungyong.helpmebackend.project.application.port.out.ProjectDeletionPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.project.domain.exception.ProjectErrorCode;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectDeletionService implements ProjectDeletionPortIn {
    private final ProjectAccessResolver projectAccessResolver;
    private final ProjectDeletionPortOut deletionPortOut;
    private final ProjectDeletionProcessor deletionProcessor;

    @Value("${workers.deletion.grace-period-seconds:300}")
    private long gracePeriodSeconds;

    @Value("${workers.deletion.retry-delay-seconds:60}")
    private long retryDelaySeconds;

    @Override
    public ProjectDeletionResult requestDeletion(DeleteProjectCommand command) {
        validate(command);
        Project project = projectAccessResolver.resolveOwned(command.userId(), command.projectId());
        if (!project.isActive()) {
            throw new CustomException(ProjectErrorCode.PROJECT_NOT_ACTIVE);
        }
        if (!project.getRepoFullName().equals(command.confirmationRepoFullname().trim())) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }

        Project requested = deletionPortOut.requestDeletion(project.getId(), now());
        return new ProjectDeletionResult(
                requested.getId(),
                requested.getStatus().getDatabaseValue(),
                requested.getDeletion().requestedAt()
        );
    }

    @Override
    public void processNext() {
        OffsetDateTime now = now();
        deletionPortOut.claimNext(
                now,
                now.minusSeconds(gracePeriodSeconds),
                now.minusSeconds(retryDelaySeconds)
        ).ifPresent(this::cleanup);
    }

    private void cleanup(Project project) {
        try {
            deletionProcessor.cleanup(project);
        } catch (RuntimeException exception) {
            deletionPortOut.markFailed(
                    project.getId(), project.getUpdatedAt(),
                    ProjectErrorCode.PROJECT_DELETION_FAILED.getErrorCode(),
                    ProjectErrorCode.PROJECT_DELETION_FAILED.getMessage()
            );
            log.warn("Project deletion cleanup failed: projectId={}, type={}",
                    project.getId(), exception.getClass().getSimpleName());
        }
    }

    private void validate(DeleteProjectCommand command) {
        if (command == null || command.userId() == null || command.projectId() == null
                || !StringUtils.hasText(command.confirmationRepoFullname())) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
