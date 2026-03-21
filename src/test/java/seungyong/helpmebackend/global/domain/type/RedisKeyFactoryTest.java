package seungyong.helpmebackend.global.domain.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisKeyFactoryTest {
    private final String OWNER = "seungyong";
    private final String NAME = "helpme";
    private final String SHA = "abc123456789";

    @Test
    @DisplayName("createReadmeKey - README 캐시 키 생성")
    void createReadmeKey() {
        String key = RedisKeyFactory.createReadmeKey(OWNER, NAME, SHA);
        assertThat(key).isEqualTo("gh:readme:seungyong:helpme:abc123456789");
    }

    @Test
    @DisplayName("createCommitsKey - Commits 캐시 키 생성")
    void createCommitsKey() {
        String key = RedisKeyFactory.createCommitsKey(OWNER, NAME, SHA);
        assertThat(key).isEqualTo("gh:commits:seungyong:helpme:abc123456789");
    }

    @Test
    @DisplayName("createLanguageKey - Language 캐시 키 생성")
    void createLanguageKey() {
        String key = RedisKeyFactory.createLanguageKey(OWNER, NAME, SHA);
        assertThat(key).isEqualTo("gh:languages:seungyong:helpme:abc123456789");
    }

    @Test
    @DisplayName("createTreeKey - Tree 캐시 키 생성")
    void createTreeKey() {
        String key = RedisKeyFactory.createTreeKey(OWNER, NAME, SHA);
        assertThat(key).isEqualTo("gh:trees:seungyong:helpme:abc123456789");
    }

    @Test
    @DisplayName("createRepoInfoKey - Repository Info (Tech Stack) 캐시 키 생성")
    void createRepoInfoKey() {
        String key = RedisKeyFactory.createRepoInfoKey(OWNER, NAME, SHA);
        assertThat(key).isEqualTo("gh:tech-stack:seungyong:helpme:abc123456789");
    }

    @Test
    @DisplayName("createEntryFileKey - Entry File 캐시 키 생성")
    void createEntryFileKey() {
        String key = RedisKeyFactory.createEntryFileKey(OWNER, NAME, SHA);
        assertThat(key).isEqualTo("gh:file:entry:seungyong:helpme:abc123456789");
    }

    @Test
    @DisplayName("createImportanceFileKey - Importance File 캐시 키 생성")
    void createImportanceFileKey() {
        String key = RedisKeyFactory.createImportanceFileKey(OWNER, NAME, SHA);
        assertThat(key).isEqualTo("gh:file:importance:seungyong:helpme:abc123456789");
    }
}