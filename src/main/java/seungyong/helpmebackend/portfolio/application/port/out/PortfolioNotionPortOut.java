package seungyong.helpmebackend.portfolio.application.port.out;

import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportDocument;
import seungyong.helpmebackend.portfolio.domain.type.PortfolioConflictAction;

public interface PortfolioNotionPortOut {
    NotionExportResult export(Long notionConnectionId, String parentPageId,
                              PortfolioExportDocument document, boolean checkTitleConflict,
                              PortfolioConflictAction conflictAction, String conflictPageId);

}
