package seungyong.helpmebackend.portfolio.adapter.in.web.dto.response;

import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExportPage;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record ResponsePortfolioExports(List<ResponsePortfolioExport> items, Page page) {
    public static ResponsePortfolioExports from(PortfolioExportPage exports) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return new ResponsePortfolioExports(exports.items().stream()
                .map(item -> ResponsePortfolioExport.from(item, now)).toList(),
                new Page(exports.nextCursor(), exports.hasNext()));
    }

    public record Page(String nextCursor, boolean hasNext) {
    }
}
