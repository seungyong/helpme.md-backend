package seungyong.helpmebackend.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.user.application.port.in.UserDeletionPortIn;

@Component
@RequiredArgsConstructor
public class UserDeletionWorker {
    private final UserDeletionPortIn userDeletionPortIn;

    @Scheduled(fixedDelayString = "${workers.deletion.fixed-delay-ms:5000}")
    public void runOnce() {
        userDeletionPortIn.processNext();
    }
}
