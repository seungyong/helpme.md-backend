package seungyong.helpmebackend.portfolio.domain.entity;

import java.time.OffsetDateTime;

public record PortfolioDownload(String url, String fileName, OffsetDateTime expiresAt) {
}
