package seungyong.helpmebackend.project.domain.type;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ProjectListStatus {
    ACTIVE("active"),
    ATTENTION_REQUIRED("attention_required");

    private final String apiValue;

    ProjectListStatus(String apiValue) {
        this.apiValue = apiValue;
    }

    public static ProjectListStatus fromApiValue(String value) {
        return Arrays.stream(values())
                .filter(status -> status.apiValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown project list status: " + value
                ));
    }
}
