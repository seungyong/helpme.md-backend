package seungyong.helpmebackend.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.notion.application.port.in.NotionDeletionPortIn;
import seungyong.helpmebackend.project.application.port.out.ProjectDeletionPortOut;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.user.application.port.out.UserDeletionPortOut;
import seungyong.helpmebackend.user.domain.entity.User;

@Component
@RequiredArgsConstructor
public class UserDeletionProcessor {
    private final UserDeletionPortOut deletionPortOut;
    private final ProjectDeletionPortOut projectDeletionPortOut;
    private final ProjectPortOut projectPortOut;
    private final NotionDeletionPortIn notionDeletionPortIn;

    @Transactional
    public void cleanup(User user) {
        // Notion revoke와 hard delete 중에는 다른 삭제 Worker의 claim을 행 잠금으로 차단
        if (!deletionPortOut.lockClaim(user.getId(), user.getUpdatedAt())) return;
        if (projectPortOut.countByUserId(user.getId()) > 0) {
            projectDeletionPortOut.requestAllByUserId(user.getId(), user.getDeletion().requestedAt());
            return;
        }
        notionDeletionPortIn.deleteUserConnection(user.getId());
        deletionPortOut.hardDelete(user.getId());
    }
}
