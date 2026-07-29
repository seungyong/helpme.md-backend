package seungyong.helpmebackend.portfolio.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import seungyong.helpmebackend.global.domain.type.DatabaseValueEnum;

@Getter
@RequiredArgsConstructor
public enum PortfolioTone implements DatabaseValueEnum {
    CONCISE("concise"),
    REFLECTION("reflection");

    private final String databaseValue;
}
