package seungyong.helpmebackend.portfolio.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

@Getter
@RequiredArgsConstructor
public enum PortfolioConflictAction implements DatabaseValueEnum {
    UPDATE("update"),
    COPY("copy");

    private final String databaseValue;
}
