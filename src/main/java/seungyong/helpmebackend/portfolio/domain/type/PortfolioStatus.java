package seungyong.helpmebackend.portfolio.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

@Getter
@RequiredArgsConstructor
public enum PortfolioStatus implements DatabaseValueEnum {
    QUEUED("queued"),
    GENERATING("generating"),
    DRAFT("draft"),
    SAVED("saved"),
    FAILED("failed");

    private final String databaseValue;
}
