package seungyong.helpmebackend.project.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.project.application.port.in.ProjectDeletionPortIn;

@Component
@RequiredArgsConstructor
public class ProjectDeletionWorker {
    private final ProjectDeletionPortIn projectDeletionPortIn;

    @Scheduled(fixedDelayString = "${workers.deletion.fixed-delay-ms:5000}")
    public void runOnce() {
        projectDeletionPortIn.processNext();
    }
}
