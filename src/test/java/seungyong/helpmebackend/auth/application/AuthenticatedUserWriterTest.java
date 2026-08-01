package seungyong.helpmebackend.auth.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.auth.application.port.out.result.OAuthGithubUser;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.GithubUser;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.user.domain.exception.UserErrorCode;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static seungyong.helpmebackend.support.fixture.TestFixtures.oauthGithubUser;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserWriterTest {
    @Mock private UserPortOut userPortOut;
    @Mock private CipherPortOut cipherPortOut;

    private AuthenticatedUserWriter writer;
    private final OffsetDateTime authenticatedAt = OffsetDateTime.of(
            2026, 8, 1, 0, 0, 0, 0, ZoneOffset.UTC
    );

    @BeforeEach
    void setUp() {
        writer = new AuthenticatedUserWriter(userPortOut, cipherPortOut);
    }

    @Test
    @DisplayName("신규 GitHub 사용자를 등록한다")
    void authenticate_registersNewUser() {
        OAuthGithubUser oauthUser = oauthGithubUser();
        given(userPortOut.getByGithubId(oauthUser.githubId())).willReturn(Optional.empty());
        given(cipherPortOut.encrypt("access-token")).willReturn("encrypted-token");
        given(userPortOut.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        writer.authenticate(oauthUser, "access-token", authenticatedAt);

        verify(userPortOut).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getGithubUser().getName()).isEqualTo(oauthUser.name());
        assertThat(userCaptor.getValue().getGithubUser().getGithubToken().value())
                .isEqualTo("encrypted-token");
    }

    @Test
    @DisplayName("기존 활성 사용자의 인증 정보를 갱신한다")
    void authenticate_updatesExistingUser() {
        OAuthGithubUser oauthUser = oauthGithubUser();
        User existing = org.mockito.Mockito.mock(User.class);
        given(userPortOut.getByGithubId(oauthUser.githubId())).willReturn(Optional.of(existing));
        given(existing.isAuthenticationAllowed()).willReturn(true);
        given(cipherPortOut.encrypt("access-token")).willReturn("encrypted-token");
        given(userPortOut.save(existing)).willReturn(existing);

        assertThat(writer.authenticate(oauthUser, "access-token", authenticatedAt))
                .isSameAs(existing);

        verify(existing).recordSuccessfulLogin(any(GithubUser.class), any(OffsetDateTime.class));
        verify(userPortOut).save(existing);
    }

    @Test
    @DisplayName("탈퇴 처리 중인 사용자는 토큰을 저장하지 않는다")
    void authenticate_rejectsDeletingUser() {
        OAuthGithubUser oauthUser = oauthGithubUser();
        User existing = org.mockito.Mockito.mock(User.class);
        given(userPortOut.getByGithubId(oauthUser.githubId())).willReturn(Optional.of(existing));
        given(existing.isAuthenticationAllowed()).willReturn(false);

        assertThatThrownBy(() -> writer.authenticate(oauthUser, "access-token", authenticatedAt))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_DELETION_IN_PROGRESS);

        verify(cipherPortOut, never()).encrypt(any());
        verify(userPortOut, never()).save(any());
    }
}
