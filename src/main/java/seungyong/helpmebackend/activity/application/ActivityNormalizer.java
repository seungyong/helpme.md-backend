package seungyong.helpmebackend.activity.application;

import org.springframework.stereotype.Component;
import seungyong.helpmebackend.activity.domain.entity.Activity;
import seungyong.helpmebackend.activity.domain.entity.ActivitySeed;
import seungyong.helpmebackend.project.domain.entity.Project;

import java.util.List;

@Component
public class ActivityNormalizer {
    public List<Activity> normalize(
            Project project,
            Long webhookDeliveryId,
            List<ActivitySeed> seeds
    ) {
        return seeds.stream()
                .map(seed -> Activity.builder()
                        .projectId(project.getId())
                        .webhookDeliveryId(webhookDeliveryId)
                        .externalKey(seed.externalKey())
                        .type(seed.type())
                        .branchName(seed.branchName())
                        .commitSha(seed.commitSha())
                        .title(seed.title())
                        .summary(seed.summary())
                        .actorLogin(seed.actorLogin())
                        .publicUrl(project.isPrivateRepository() ? null : seed.publicUrl())
                        .additions(seed.additions())
                        .deletions(seed.deletions())
                        .filesChanged(seed.filesChanged())
                        .occurredAt(seed.occurredAt())
                        .details(seed.details())
                        .build())
                .toList();
    }
}
