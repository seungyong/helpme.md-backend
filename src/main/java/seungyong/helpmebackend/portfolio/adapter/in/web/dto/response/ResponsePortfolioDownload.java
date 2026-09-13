package seungyong.helpmebackend.portfolio.adapter.in.web.dto.response;

import seungyong.helpmebackend.portfolio.domain.entity.PortfolioDownload;

import java.time.OffsetDateTime;

public record ResponsePortfolioDownload(String downloadUrl, String fileName, OffsetDateTime expiresAt) {
    public static ResponsePortfolioDownload from(PortfolioDownload download) {
        return new ResponsePortfolioDownload(download.url(), download.fileName(), download.expiresAt());
    }
}
