package seungyong.helpmebackend.portfolio.application.port.out;

public record NotionPageWriteResult(boolean conflict, String pageId, String pageTitle, String pageUrl) {
}
