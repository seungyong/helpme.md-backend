package seungyong.helpmebackend.notion.application.port.in;

import seungyong.helpmebackend.notion.application.port.in.command.StartNotionAuthorizationCommand;
import seungyong.helpmebackend.notion.application.port.in.command.UpdateNotionDefaultPageCommand;
import seungyong.helpmebackend.notion.application.port.in.result.NotionAuthorizationResult;
import seungyong.helpmebackend.notion.application.port.in.result.NotionCallbackResult;
import seungyong.helpmebackend.notion.application.port.in.result.UpdatedNotionDefaultPage;
import seungyong.helpmebackend.notion.domain.entity.NotionConnection;
import seungyong.helpmebackend.notion.domain.entity.NotionPageCandidates;

public interface NotionPortIn {
    NotionAuthorizationResult startAuthorization(StartNotionAuthorizationCommand command);

    NotionCallbackResult handleCallback(String code, String state, String providerError);

    NotionConnection getConnection(Long userId);

    NotionPageCandidates getPages(
            Long userId, String query, String cursor, Integer size
    );

    UpdatedNotionDefaultPage updateDefaultPage(UpdateNotionDefaultPageCommand command);

    void disconnect(Long userId);
}
