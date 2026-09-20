package seungyong.helpmebackend.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.project.application.port.out.ProjectDeletionPortOut;
import seungyong.helpmebackend.user.application.port.out.UserDeletionPortOut;

import seungyong.helpmebackend.user.domain.entity.User;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class UserDeletionWriter {

    private final UserDeletionPortOut userDeletionPortOut;
    private final ProjectDeletionPortOut projectDeletionPortOut;

    @Transactional
    public User request(Long userId, OffsetDateTime requestedAt) {
        // 최초 요청 시각 판정은 persistence의 사용자 행 잠금 안에서 수행
        User requested = userDeletionPortOut.requestDeletion(userId, requestedAt);
        // 사용자 차단과 모든 프로젝트의 producer 차단을 같은 transaction에서 확정
        projectDeletionPortOut.requestAllByUserId(userId, requested.getDeletion().requestedAt());
        return requested;
    }
}
