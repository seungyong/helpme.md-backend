package seungyong.helpmebackend.project.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.webhook.application.port.out.WebhookWorkPortOut;

@Component
@RequiredArgsConstructor
class ProjectCreationWriter {
    private final ProjectPortOut projectPortOut;
    private final WebhookWorkPortOut webhookWorkPortOut;

    @Transactional
    public Project create(Project project) {
        Project saved = projectPortOut.save(project);
        webhookWorkPortOut.registerInitialSync(saved.getId());
        return saved;
    }
}
