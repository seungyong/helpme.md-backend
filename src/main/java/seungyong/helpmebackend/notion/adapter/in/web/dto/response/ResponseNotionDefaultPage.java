package seungyong.helpmebackend.notion.adapter.in.web.dto.response;

import seungyong.helpmebackend.notion.application.port.in.result.UpdatedNotionDefaultPage;

import java.time.OffsetDateTime;

public record ResponseNotionDefaultPage(
        ParentPage defaultParentPage,
        OffsetDateTime updatedAt
) {
    public static ResponseNotionDefaultPage from(UpdatedNotionDefaultPage page) {
        return new ResponseNotionDefaultPage(
                new ParentPage(page.id(), page.title()), page.updatedAt()
        );
    }

    public record ParentPage(String id, String title) { }
}
