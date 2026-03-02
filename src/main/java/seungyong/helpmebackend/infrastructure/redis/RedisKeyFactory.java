package seungyong.helpmebackend.infrastructure.redis;

import lombok.AllArgsConstructor;

import java.util.Arrays;

@AllArgsConstructor
public enum RedisKeyFactory {
    // commit cache
    COMMITS_KEY("gh:commits:"),

    // repository cache
    README_KEY("gh:readme:"),
    LANGUAGE_KEY("gh:languages:"),
    TREE_KEY("gh:trees:"),
    TECH_STACK_KEY("gh:tech-stack:"),

    // file content
    FILE_V1_KEY("gh:file:entry:"),
    FILE_V2_KEY("gh:file:importance:");

    private final String prefix;

    // 공통 조합 메서드
    private String buildKey(Object... parts) {
        return prefix + String.join(":",
                Arrays.stream(parts)
                        .map(String::valueOf)
                        .toList()
        );
    }

    public static String createReadmeKey(String owner, String name, String sha) {
        return README_KEY.buildKey(owner, name, sha);
    }

    public static String createCommitsKey(String owner, String name, String sha) {
        return COMMITS_KEY.buildKey(owner, name, sha);
    }

    public static String createLanguageKey(String owner, String name, String sha) {
        return LANGUAGE_KEY.buildKey(owner, name, sha);
    }

    public static String createTreeKey(String owner, String name, String sha) {
        return TREE_KEY.buildKey(owner, name, sha);
    }

    public static String createRepoInfoKey(String owner, String name, String sha) {
        return TECH_STACK_KEY.buildKey(owner, name, sha);
    }

    public static String createEntryFileKey(String owner, String name, String sha) {
        return FILE_V1_KEY.buildKey(owner, name, sha);
    }

    public static String createImportanceFileKey(String owner, String name, String sha) {
        return FILE_V2_KEY.buildKey(owner, name, sha);
    }
}
