package seungyong.helpmebackend.user.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.global.domain.type.RedisKey;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;

import seungyong.helpmebackend.user.application.port.in.UserDeletionPortIn;
import seungyong.helpmebackend.user.application.port.in.command.DeleteUserCommand;
import seungyong.helpmebackend.user.application.port.in.result.UserDeletionResult;
import seungyong.helpmebackend.user.application.port.out.UserDeletionPortOut;
import seungyong.helpmebackend.user.domain.entity.User;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeletionService implements UserDeletionPortIn {
    private final UserDeletionWriter deletionWriter;
    private final UserDeletionPortOut userDeletionPortOut;
    private final UserDeletionProcessor deletionProcessor;
    private final RedisPortOut redisPortOut;

    @Value("${workers.deletion.grace-period-seconds:300}")
    private long gracePeriodSeconds;

    @Value("${workers.deletion.retry-delay-seconds:60}")
    private long retryDelaySeconds;

    @Override
    public UserDeletionResult requestDeletion(DeleteUserCommand command) {
        if (command == null || command.userId() == null || !command.confirmed()) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }

        User requested = deletionWriter.request(command.userId(), now());
        deleteCurrentRefreshToken(command.userId(), command.refreshToken());
        return new UserDeletionResult(
                "deleting", requested.getDeletion().requestedAt(), "sign_out"
        );
    }

    @Override
    public void processNext() {
        OffsetDateTime now = now();
        userDeletionPortOut.claimNext(
                now,
                now.minusSeconds(gracePeriodSeconds),
                now.minusSeconds(retryDelaySeconds)
        ).ifPresent(this::cleanup);
    }

    private void cleanup(User user) {
        try {
            deletionProcessor.cleanup(user);
        } catch (RuntimeException exception) {
            userDeletionPortOut.markFailed(
                    user.getId(), user.getUpdatedAt(),
                    GlobalErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(),
                    "회원 탈퇴 외부 자산 정리에 실패했습니다."
            );
            log.warn("User deletion cleanup failed: userId={}, type={}",
                    user.getId(), exception.getClass().getSimpleName());
        }
    }

    private void deleteCurrentRefreshToken(Long userId, String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }
        try {
            redisPortOut.delete(RedisKey.REFRESH_KEY.getValue() + refreshToken);
        } catch (RuntimeException exception) {
            // DB guard가 모든 session을 즉시 차단하므로 Redis 정리는 best-effort 처리
            log.warn("Current refresh token cleanup failed during deletion: userId={}", userId);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
