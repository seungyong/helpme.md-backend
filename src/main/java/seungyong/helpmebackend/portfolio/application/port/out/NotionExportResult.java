package seungyong.helpmebackend.portfolio.application.port.out;

public record NotionExportResult(boolean needsAction, String pageId, String pageTitle, String pageUrl) {
    public static NotionExportResult conflict(String pageId, String pageTitle, String pageUrl) {
        return new NotionExportResult(true, pageId, pageTitle, pageUrl);
    }

    public static NotionExportResult succeeded(String pageId, String pageUrl) {
        return new NotionExportResult(false, pageId, null, pageUrl);
    }
}
