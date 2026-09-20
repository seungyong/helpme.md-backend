package seungyong.helpmebackend.project.adapter.out.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import seungyong.helpmebackend.project.application.port.out.ProjectDeletionPortOut;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.support.repository.JpaTest;
import seungyong.helpmebackend.user.application.port.out.UserDeletionPortOut;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;

import java.time.OffsetDateTime;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static seungyong.helpmebackend.support.fixture.TestFixtures.project;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

@JpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DeletionConcurrencyTest {
    @Autowired private UserPortOut users;
    @Autowired private UserDeletionPortOut userDeletion;
    @Autowired private ProjectPortOut projects;
    @Autowired private ProjectDeletionPortOut deletion;
    @Autowired private PlatformTransactionManager transactionManager;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private Long userId;

    @AfterEach
    void cleanup() throws Exception {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        if (userId != null) userDeletion.hardDelete(userId);
    }

    @Test
    @DisplayName("두 트랜잭션의 동시 탈퇴 요청은 최초 requestedAt을 동일하게 반환")
    void preservesFirstRequestAcrossTransactions() throws Exception {
        userId = users.save(user(null, "concurrent-delete-token")).getId();
        OffsetDateTime firstAt = OffsetDateTime.now().minusMinutes(1);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Future<OffsetDateTime> first = executor.submit(() -> tx.execute(status -> {
            var result = userDeletion.requestDeletion(userId, firstAt);
            locked.countDown();
            await(release);
            return result.getDeletion().requestedAt();
        }));
        assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
        Future<OffsetDateTime> second = executor.submit(() ->
                userDeletion.requestDeletion(userId, firstAt.plusSeconds(1)).getDeletion().requestedAt());
        try {
            assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
        } finally {
            release.countDown();
        }
        assertThat(second.get(5, TimeUnit.SECONDS).toInstant())
                .isEqualTo(first.get(5, TimeUnit.SECONDS).toInstant());
    }

    @Test
    @DisplayName("정리가 60초를 넘어도 행 잠금 보유 중에는 다음 Worker가 작업을 가져가지 못함")
    void cleanupLockOutlivesRetryWindow() throws Exception {
        userId = users.save(user(null, "cleanup-lock-token")).getId();
        var project = projects.save(project(userId));
        deletion.requestDeletion(project.getId(), OffsetDateTime.now().minusMinutes(20));
        OffsetDateTime clock = OffsetDateTime.now().plusMinutes(10);
        var claim = deletion.claimNext(clock, clock.minusMinutes(5), clock.minusMinutes(1)).orElseThrow();
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<?> owner = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertThat(deletion.lockClaim(claim.getId(), claim.getUpdatedAt())).isTrue();
            locked.countDown();
            await(release);
            deletion.hardDelete(claim.getId());
        }));
        assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
        Future<?> contender = executor.submit(() -> deletion.claimNext(
                clock.plusMinutes(2), clock.minusMinutes(3), clock.plusMinutes(1)));
        try {
            assertThatThrownBy(() -> contender.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
        } finally {
            release.countDown();
        }
        owner.get(5, TimeUnit.SECONDS);
        assertThat(contender.get(5, TimeUnit.SECONDS)).isEqualTo(java.util.Optional.empty());
    }

    @Test
    @DisplayName("실행 전 소유권이 교체된 오래된 claim은 정리와 실패 상태 반영을 할 수 없음")
    void rejectsStaleClaim() {
        userId = users.save(user(null, "stale-claim-token")).getId();
        var project = projects.save(project(userId));
        deletion.requestDeletion(project.getId(), OffsetDateTime.now().minusMinutes(20));
        OffsetDateTime clock = OffsetDateTime.now().plusMinutes(10);
        var first = deletion.claimNext(clock, clock.minusMinutes(5), clock.minusMinutes(1)).orElseThrow();
        var second = deletion.claimNext(clock.plusMinutes(2), clock.minusMinutes(3), clock.plusMinutes(1)).orElseThrow();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertThat(deletion.lockClaim(first.getId(), first.getUpdatedAt())).isFalse();
            assertThat(deletion.lockClaim(second.getId(), second.getUpdatedAt())).isTrue();
        });
        deletion.markFailed(first.getId(), first.getUpdatedAt(), "PROJECT_50002", "stale");
        assertThat(projects.getById(project.getId()).orElseThrow().getDeletion().error()).isNull();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("test latch timed out");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
