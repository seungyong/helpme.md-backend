package seungyong.helpmebackend.github.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GithubAccountType {
    USER("User"),
    ORGANIZATION("Organization");

    private final String apiValue;

    public static GithubAccountType from(String value) {
        for (GithubAccountType type : values()) {
            if (type.apiValue.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 GitHub 계정 종류입니다: " + value);
    }
}
