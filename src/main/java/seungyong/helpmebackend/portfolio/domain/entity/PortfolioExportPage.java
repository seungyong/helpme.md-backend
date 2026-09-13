package seungyong.helpmebackend.portfolio.domain.entity;

import java.util.List;

public record PortfolioExportPage(List<PortfolioExport> items, String nextCursor, boolean hasNext) {
    public PortfolioExportPage {
        items = List.copyOf(items);
    }
}
