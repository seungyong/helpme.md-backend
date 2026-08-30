package seungyong.helpmebackend.notion.application;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.global.domain.type.RedisKey;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.notion.application.port.out.exception.NotionProviderException;
import seungyong.helpmebackend.notion.application.port.in.NotionPortIn;
import seungyong.helpmebackend.notion.application.port.in.command.StartNotionAuthorizationCommand;
import seungyong.helpmebackend.notion.application.port.in.command.UpdateNotionDefaultPageCommand;
import seungyong.helpmebackend.notion.application.port.in.result.NotionAuthorizationResult;
import seungyong.helpmebackend.notion.application.port.in.result.NotionCallbackResult;
import seungyong.helpmebackend.notion.application.port.in.result.UpdatedNotionDefaultPage;
import seungyong.helpmebackend.notion.application.port.out.NotionConnectionPortOut;
import seungyong.helpmebackend.notion.application.port.out.NotionProviderPortOut;
import seungyong.helpmebackend.notion.application.port.out.result.NotionOAuthGrant;
import seungyong.helpmebackend.notion.application.port.out.result.NotionProviderPage;
import seungyong.helpmebackend.notion.application.port.out.result.NotionProviderPages;
import seungyong.helpmebackend.notion.application.port.out.result.NotionRefreshedTokens;
import seungyong.helpmebackend.notion.domain.entity.NotionAuthorization;
import seungyong.helpmebackend.notion.domain.entity.NotionConnection;
import seungyong.helpmebackend.notion.domain.entity.NotionPageCandidate;
import seungyong.helpmebackend.notion.domain.entity.NotionPageCandidates;
import seungyong.helpmebackend.notion.domain.exception.NotionErrorCode;
import seungyong.helpmebackend.notion.domain.exception.NotionRateLimitException;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.user.domain.exception.UserErrorCode;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotionService implements NotionPortIn {
    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAX_PAGE_SIZE = 100;
    private static final TypeReference<NotionOAuthState> STATE_TYPE = new TypeReference<>() { };

    private final RedisPortOut redisPortOut;
    private final UserPortOut userPortOut;
    private final NotionConnectionPortOut notionConnectionPortOut;
    private final NotionProviderPortOut notionProviderPortOut;
    private final CipherPortOut cipherPortOut;

    @Value("${oauth2.notion.allowed-return-url:/settings/integrations}")
    private String allowedReturnUrl;

    @Value("${oauth2.notion.state-ttl-seconds:600}")
    private long stateTtlSeconds;

    @Override
    public NotionAuthorizationResult startAuthorization(StartNotionAuthorizationCommand command) {
        ensureActiveUser(command.userId());
        String returnUrl = normalizeReturnUrl(command.returnUrl());
        String state = UUID.randomUUID().toString();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(stateTtlSeconds);
        redisPortOut.setObjectIfAbsent(
                stateKey(state),
                new NotionOAuthState(command.userId(), returnUrl),
                expiresAt.toInstant()
        );
        try {
            return new NotionAuthorizationResult(
                    notionProviderPortOut.buildAuthorizationUrl(state),
                    expiresAt
            );
        } catch (NotionProviderException e) {
            redisPortOut.delete(stateKey(state));
            throw new CustomException(NotionErrorCode.NOTION_UPSTREAM_ERROR);
        }
    }

    @Override
    public NotionCallbackResult handleCallback(String code, String state, String providerError) {
        NotionOAuthState oauthState = consumeState(state);
        String returnUrl = oauthState == null ? allowedReturnUrl : oauthState.returnUrl();
        if (oauthState == null) {
            return NotionCallbackResult.error(
                    returnUrl, GlobalErrorCode.OAUTH_STATE_INVALID.getErrorCode()
            );
        }
        if (StringUtils.hasText(providerError)) {
            return NotionCallbackResult.denied(returnUrl);
        }
        if (!StringUtils.hasText(code)) {
            return NotionCallbackResult.error(
                    returnUrl, GlobalErrorCode.OAUTH_STATE_INVALID.getErrorCode()
            );
        }

        try {
            ensureActiveUser(oauthState.userId());
            NotionOAuthGrant grant = notionProviderPortOut.exchangeAuthorizationCode(code);
            NotionConnection current = notionConnectionPortOut.getByUserId(oauthState.userId())
                    .orElse(null);

            // 가져온 grant의 workspaceId와 현재 연결된 workspaceId가 다르면 기존 연결을 해제하고 새로 연결
            if (current != null && !current.isSameWorkspace(grant.getWorkspaceId())) {
                withRefresh(current, token -> {
                    notionProviderPortOut.revokeAccessToken(token);
                    return null;
                }, NotionErrorCode.NOTION_CONNECTION_EXPIRED,
                        NotionErrorCode.NOTION_UPSTREAM_ERROR);
            }

            OffsetDateTime now = OffsetDateTime.now();
            notionConnectionPortOut.saveAuthorization(
                    oauthState.userId(),
                    new NotionAuthorization(
                            grant.getWorkspaceId(), grant.getWorkspaceName(), grant.getBotId(),
                            grant.getOwnerName(), grant.getOwnerEmail(),
                            cipherPortOut.encrypt(grant.getAccessToken()),
                            cipherPortOut.encrypt(grant.getRefreshToken()), now
                    )
            );
            return NotionCallbackResult.success(returnUrl);
        } catch (CustomException e) {
            return NotionCallbackResult.error(returnUrl, e.getErrorCode().getErrorCode());
        } catch (NotionProviderException e) {
            return NotionCallbackResult.error(
                    returnUrl, NotionErrorCode.NOTION_UPSTREAM_ERROR.getErrorCode()
            );
        } catch (RuntimeException e) {
            return NotionCallbackResult.error(
                    returnUrl, GlobalErrorCode.INTERNAL_SERVER_ERROR.getErrorCode()
            );
        }
    }

    @Override
    public NotionConnection getConnection(Long userId) {
        return notionConnectionPortOut.getByUserId(userId)
                .orElseGet(() -> NotionConnection.disconnected(userId));
    }

    @Override
    public NotionPageCandidates getPages(
            Long userId, String query, String cursor, Integer requestedSize
    ) {
        int size = requestedSize == null ? DEFAULT_PAGE_SIZE : requestedSize;
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
        NotionConnection connection = requireConnection(userId);
        NotionProviderPages result = withRefresh(
                connection,
                token -> notionProviderPortOut.searchPages(token, query, cursor, size),
                NotionErrorCode.NOTION_CONNECTION_EXPIRED,
                NotionErrorCode.NOTION_UPSTREAM_ERROR
        );
        notionConnectionPortOut.recordVerification(userId, OffsetDateTime.now());
        List<NotionPageCandidate> items = result.items().stream()
                .map(page -> new NotionPageCandidate(
                        page.id(), page.title(),
                        connection.getWorkspaceName() + " / " + page.title(),
                        page.lastEditedAt()
                ))
                .toList();
        return new NotionPageCandidates(items, result.nextCursor(), result.hasNext());
    }

    @Override
    public UpdatedNotionDefaultPage updateDefaultPage(
            UpdateNotionDefaultPageCommand command
    ) {
        if (!StringUtils.hasText(command.defaultParentPageId())
                || !StringUtils.hasText(command.defaultParentPageTitle())) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
        NotionConnection connection = requireConnection(command.userId());
        NotionProviderPage providerPage = withRefresh(
                connection,
                token -> notionProviderPortOut.retrievePage(
                        token, command.defaultParentPageId().trim()
                ),
                NotionErrorCode.NOTION_PAGE_ACCESS_DENIED,
                NotionErrorCode.NOTION_PARENT_PAGE_NOT_FOUND
        );
        OffsetDateTime now = OffsetDateTime.now();
        NotionConnection updated = notionConnectionPortOut.updateDefaultPage(
                command.userId(), providerPage.id(),
                command.defaultParentPageTitle().trim(), now
        );
        return new UpdatedNotionDefaultPage(
                updated.getDefaultParentPageId(),
                updated.getDefaultParentPageTitle(),
                updated.getUpdatedAt() == null ? now : updated.getUpdatedAt()
        );
    }

    @Override
    public void disconnect(Long userId) {
        NotionConnection connection = requireConnection(userId);
        withRefresh(connection, token -> {
            notionProviderPortOut.revokeAccessToken(token);
            return null;
        }, NotionErrorCode.NOTION_CONNECTION_EXPIRED, NotionErrorCode.NOTION_UPSTREAM_ERROR);
        notionConnectionPortOut.deleteByUserId(userId);
    }

    private NotionConnection requireConnection(Long userId) {
        NotionConnection connection = notionConnectionPortOut.getByUserId(userId)
                .orElseThrow(() -> new CustomException(
                        NotionErrorCode.NOTION_CONNECTION_NOT_FOUND
                ));
        if (!connection.isConnected() || !connection.hasTokenPair()) {
            throw new CustomException(NotionErrorCode.NOTION_CONNECTION_EXPIRED);
        }
        return connection;
    }

    private <T> T withRefresh(
            NotionConnection connection,
            TokenRequest<T> request,
            NotionErrorCode forbiddenCode,
            NotionErrorCode notFoundCode
    ) {
        // 첫 번째 시도: 기존 access token으로 요청 (기존 access token이 유효한 경우)
        try {
            return request.execute(cipherPortOut.decrypt(connection.getEncryptedAccessToken()));
        } catch (NotionProviderException first) {
            if (first.getFailure() != NotionProviderException.Failure.UNAUTHORIZED) {
                throw mapped(connection, first, forbiddenCode, notFoundCode);
            }
        }

        // 두 번째 시도: refresh token으로 access token 갱신 후 재시도 (기존 access token이 만료된 경우)
        try {
            NotionRefreshedTokens refreshed = notionProviderPortOut.refreshAccessToken(
                    cipherPortOut.decrypt(connection.getEncryptedRefreshToken())
            );
            notionConnectionPortOut.rotateTokens(
                    connection.getUserId(),
                    cipherPortOut.encrypt(refreshed.getAccessToken()),
                    cipherPortOut.encrypt(refreshed.getRefreshToken()),
                    OffsetDateTime.now()
            );
            return request.execute(refreshed.getAccessToken());
        } catch (NotionProviderException retryFailure) {
            if (retryFailure.getFailure() == NotionProviderException.Failure.UNAUTHORIZED) {
                markReconnectRequired(connection);
                throw new CustomException(NotionErrorCode.NOTION_CONNECTION_EXPIRED);
            }
            throw mapped(connection, retryFailure, forbiddenCode, notFoundCode);
        }
    }

    private RuntimeException mapped(
            NotionConnection connection,
            NotionProviderException exception,
            NotionErrorCode forbiddenCode,
            NotionErrorCode notFoundCode
    ) {
        if (exception.getFailure() == NotionProviderException.Failure.FORBIDDEN
                && forbiddenCode == NotionErrorCode.NOTION_CONNECTION_EXPIRED) {
            markReconnectRequired(connection);
        }
        return switch (exception.getFailure()) {
            case FORBIDDEN -> new CustomException(forbiddenCode);
            case NOT_FOUND -> new CustomException(notFoundCode);
            case RATE_LIMIT -> new NotionRateLimitException(exception.getRetryAfterSeconds());
            case BAD_REQUEST -> new CustomException(GlobalErrorCode.BAD_REQUEST);
            case UNAUTHORIZED -> new CustomException(NotionErrorCode.NOTION_CONNECTION_EXPIRED);
            case UPSTREAM -> new CustomException(NotionErrorCode.NOTION_UPSTREAM_ERROR);
        };
    }

    private void markReconnectRequired(NotionConnection connection) {
        notionConnectionPortOut.markReconnectRequired(
                connection.getUserId(),
                NotionErrorCode.NOTION_CONNECTION_EXPIRED.getErrorCode(),
                NotionErrorCode.NOTION_CONNECTION_EXPIRED.getMessage(),
                OffsetDateTime.now()
        );
    }

    private NotionOAuthState consumeState(String state) {
        if (!StringUtils.hasText(state)) return null;
        return redisPortOut.getObjectAndDelete(stateKey(state), STATE_TYPE);
    }

    private String normalizeReturnUrl(String returnUrl) {
        if (!StringUtils.hasText(returnUrl)) return allowedReturnUrl;
        if (!allowedReturnUrl.equals(returnUrl)) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
        return returnUrl;
    }

    private void ensureActiveUser(Long userId) {
        User user = userPortOut.getById(userId);
        if (!user.isAuthenticationAllowed()) {
            throw new CustomException(UserErrorCode.USER_DELETION_IN_PROGRESS);
        }
    }

    private String stateKey(String state) {
        return RedisKey.NOTION_OAUTH_STATE_KEY.getValue() + state;
    }

    private record NotionOAuthState(Long userId, String returnUrl) { }

    @FunctionalInterface
    private interface TokenRequest<T> {
        T execute(String accessToken);
    }
}
