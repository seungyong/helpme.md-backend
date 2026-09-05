package seungyong.helpmebackend.portfolio.application.port.in.command;

public record ListPortfoliosQuery(Long userId, Long projectId, String status, String cursor, Integer size) {
}
