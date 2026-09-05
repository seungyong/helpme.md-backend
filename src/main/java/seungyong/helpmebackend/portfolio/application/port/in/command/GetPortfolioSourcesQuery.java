package seungyong.helpmebackend.portfolio.application.port.in.command;

import java.time.LocalDate;

public record GetPortfolioSourcesQuery(Long userId, Long projectId, LocalDate periodStart, LocalDate periodEnd) {
}
