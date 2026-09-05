package seungyong.helpmebackend.portfolio.domain.entity;

public record PortfolioEligibility(boolean canCreate, String reason, int requiredSavedReflectionCount, long currentSavedReflectionCount) {
    public static PortfolioEligibility from(long count) {
        return new PortfolioEligibility(count >= 1, count >= 1 ? null : "saved_reflection_required", 1, count);
    }
}
