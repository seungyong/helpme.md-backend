package seungyong.helpmebackend.notion.adapter.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import seungyong.helpmebackend.notion.application.port.out.NotionConnectionPortOut;
import seungyong.helpmebackend.notion.domain.entity.NotionAuthorization;
import seungyong.helpmebackend.notion.domain.type.NotionConnectionStatus;
import seungyong.helpmebackend.support.repository.JpaTest;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static seungyong.helpmebackend.support.fixture.TestFixtures.user;

@JpaTest
class NotionConnectionAdapterTest {
    @Autowired private NotionConnectionPortOut notionConnectionPortOut;
    @Autowired private UserPortOut userPortOut;

    @Test
    @DisplayName("같은 workspace 재연결은 row와 기본 페이지를 유지하며 token pair를 교체")
    void reconnect_sameWorkspace() {
        User savedUser = userPortOut.save(user(null, "github-token"));
        var first = notionConnectionPortOut.saveAuthorization(
                savedUser.getId(), authorization("workspace-1", "access-1", "refresh-1")
        );
        notionConnectionPortOut.updateDefaultPage(
                savedUser.getId(), "page-1", "Portfolio", OffsetDateTime.now()
        );

        var reconnected = notionConnectionPortOut.saveAuthorization(
                savedUser.getId(), authorization("workspace-1", "access-2", "refresh-2")
        );

        assertThat(reconnected.getId()).isEqualTo(first.getId());
        assertThat(reconnected.getEncryptedAccessToken()).isEqualTo("access-2");
        assertThat(reconnected.getEncryptedRefreshToken()).isEqualTo("refresh-2");
        assertThat(reconnected.getDefaultParentPageId()).isEqualTo("page-1");
        assertThat(reconnected.getStatus()).isEqualTo(NotionConnectionStatus.CONNECTED);
    }

    @Test
    @DisplayName("다른 workspace 연결은 기존 row를 제거하고 기본 페이지를 초기화")
    void reconnect_differentWorkspace() {
        User savedUser = userPortOut.save(user(null, "github-token"));
        var first = notionConnectionPortOut.saveAuthorization(
                savedUser.getId(), authorization("workspace-1", "access-1", "refresh-1")
        );
        notionConnectionPortOut.updateDefaultPage(
                savedUser.getId(), "page-1", "Portfolio", OffsetDateTime.now()
        );

        var changed = notionConnectionPortOut.saveAuthorization(
                savedUser.getId(), authorization("workspace-2", "access-2", "refresh-2")
        );

        assertThat(changed.getId()).isNotEqualTo(first.getId());
        assertThat(changed.getWorkspaceId()).isEqualTo("workspace-2");
        assertThat(changed.getDefaultParentPageId()).isNull();
    }

    private NotionAuthorization authorization(
            String workspaceId, String accessToken, String refreshToken
    ) {
        return new NotionAuthorization(
                workspaceId, "Helpme", "bot-1", "Seungyong", "user@example.com",
                accessToken, refreshToken, OffsetDateTime.parse("2026-08-23T10:00:00Z")
        );
    }
}
