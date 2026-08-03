package seungyong.helpmebackend.github.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GithubRepositorySelection {
    ALL("all"),
    SELECTED("selected");

    private final String apiValue;

    public static GithubRepositorySelection from(String value) {
        for (GithubRepositorySelection selection : values()) {
            if (selection.apiValue.equalsIgnoreCase(value)) {
                return selection;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 GitHub Repository 선택 범위입니다: " + value);
    }
}
