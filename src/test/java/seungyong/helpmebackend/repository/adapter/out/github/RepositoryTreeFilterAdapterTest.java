package seungyong.helpmebackend.repository.adapter.out.github;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryTreeResult;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class RepositoryTreeFilterAdapterTest {
    @Mock private ResourceLoader resourceLoader;

    private final String extPath = "classpath:exclude_extension.txt";
    private final String dirPath = "classpath:exclude_folder.txt";
    private final String filePath = "classpath:exclude_file.txt";

    private void mockResource(String path, String content, boolean exists) throws Exception {
        Resource resource = mock(Resource.class);
        doReturn(resource).when(resourceLoader).getResource(path);
        doReturn(exists).when(resource).exists();

        if (exists) {
            doReturn(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))).when(resource).getInputStream();
        }
    }

    // 성공 케이스들에서 공통으로 사용할 정상 어댑터 생성 헬퍼
    private RepositoryTreeFilterAdapter createValidAdapter() throws Exception {
        mockResource(extPath, ".jpg, .png, .class", true);
        mockResource(dirPath, "node_modules, target, build", true);
        mockResource(filePath, ".DS_Store, secret.txt", true);

        return new RepositoryTreeFilterAdapter(extPath, dirPath, filePath, resourceLoader);
    }

    @Nested
    @DisplayName("Constructor - 객체 초기화")
    class Constructor {
        @Test
        @DisplayName("실패 - 확장자 필터 파일이 존재하지 않는 경우")
        void constructor_failure_not_exist_extensions() throws Exception {
            mockResource(extPath, "", false);

            assertThatThrownBy(() -> new RepositoryTreeFilterAdapter(extPath, dirPath, filePath, resourceLoader))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("필터링 기준 파일이 존재하지 않습니다: " + extPath);
        }

        @Test
        @DisplayName("실패 - 확장자 필터 파일이 비어있는 경우")
        void constructor_failure_empty_extensions() throws Exception {
            mockResource(extPath, "", true);
            mockResource(dirPath, "node_modules", true);
            mockResource(filePath, ".DS_Store", true);

            assertThatThrownBy(() -> new RepositoryTreeFilterAdapter(extPath, dirPath, filePath, resourceLoader))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("필터링 기준 파일이 비어 있습니다.");
        }

        @Test
        @DisplayName("실패 - 디렉토리 필터 파일이 비어있는 경우")
        void constructor_failure_empty_directories() throws Exception {
            mockResource(extPath, ".jpg", true);
            mockResource(dirPath, "", true);
            mockResource(filePath, ".DS_Store", true);

            assertThatThrownBy(() -> new RepositoryTreeFilterAdapter(extPath, dirPath, filePath, resourceLoader))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("필터링 기준 파일이 비어 있습니다.");
        }

        @Test
        @DisplayName("실패 - 파일명 필터 파일이 비어있는 경우")
        void constructor_failure_empty_filenames() throws Exception {
            mockResource(extPath, ".jpg", true);
            mockResource(dirPath, "node_modules", true);
            mockResource(filePath, "", true);

            assertThatThrownBy(() -> new RepositoryTreeFilterAdapter(extPath, dirPath, filePath, resourceLoader))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("필터링 기준 파일이 비어 있습니다.");
        }
    }

    @Nested
    @DisplayName("filter - 저장소 트리 필터링")
    class Filter {
        @Test
        @DisplayName("성공 - 분석 가능한 유효한 파일만 필터링")
        void filter_success_valid_files() throws Exception {
            RepositoryTreeFilterAdapter adapter = createValidAdapter();
            List<RepositoryTreeResult> tree = List.of(
                    new RepositoryTreeResult("src/main/java/Main.java", "blob"),
                    new RepositoryTreeResult("README.md", "blob")
            );

            List<RepositoryTreeResult> result = adapter.filter(tree);

            assertThat(result).hasSize(2);
            assertThat(result).extracting("path").containsExactly("src/main/java/Main.java", "README.md");
        }

        @Test
        @DisplayName("성공 - blob 타입이 아닌 항목 제외")
        void filter_success_exclude_non_blob() throws Exception {
            RepositoryTreeFilterAdapter adapter = createValidAdapter();
            List<RepositoryTreeResult> tree = List.of(
                    new RepositoryTreeResult("src/main/java", "tree"),
                    new RepositoryTreeResult("src/main/java/Main.java", "blob")
            );

            List<RepositoryTreeResult> result = adapter.filter(tree);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).path()).isEqualTo("src/main/java/Main.java");
        }

        @Test
        @DisplayName("성공 - 제외 디렉토리에 포함된 파일 제외")
        void filter_success_exclude_directories() throws Exception {
            RepositoryTreeFilterAdapter adapter = createValidAdapter();
            List<RepositoryTreeResult> tree = List.of(
                    new RepositoryTreeResult("node_modules/library/index.js", "blob"),
                    new RepositoryTreeResult("project/target/classes/Main.class", "blob"),
                    new RepositoryTreeResult("src/build/output.txt", "blob"),
                    new RepositoryTreeResult("src/main/java/Main.java", "blob")
            );

            List<RepositoryTreeResult> result = adapter.filter(tree);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).path()).isEqualTo("src/main/java/Main.java");
        }

        @Test
        @DisplayName("성공 - 제외 파일명에 해당하는 파일 제외")
        void filter_success_exclude_filenames() throws Exception {
            RepositoryTreeFilterAdapter adapter = createValidAdapter();
            List<RepositoryTreeResult> tree = List.of(
                    new RepositoryTreeResult("src/.DS_Store", "blob"),
                    new RepositoryTreeResult("secret.txt", "blob"),
                    new RepositoryTreeResult("src/main/java/Main.java", "blob")
            );

            List<RepositoryTreeResult> result = adapter.filter(tree);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).path()).isEqualTo("src/main/java/Main.java");
        }

        @Test
        @DisplayName("성공 - 제외 확장자에 해당하는 파일 제외")
        void filter_success_exclude_extensions() throws Exception {
            RepositoryTreeFilterAdapter adapter = createValidAdapter();
            List<RepositoryTreeResult> tree = List.of(
                    new RepositoryTreeResult("image.jpg", "blob"),
                    new RepositoryTreeResult("icon.png", "blob"),
                    new RepositoryTreeResult("Main.class", "blob"),
                    new RepositoryTreeResult("src/main/java/Main.java", "blob")
            );

            List<RepositoryTreeResult> result = adapter.filter(tree);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).path()).isEqualTo("src/main/java/Main.java");
        }

        @Test
        @DisplayName("성공 - 확장자가 없는 파일 제외")
        void filter_success_exclude_no_extension() throws Exception {
            RepositoryTreeFilterAdapter adapter = createValidAdapter();
            List<RepositoryTreeResult> tree = List.of(
                    new RepositoryTreeResult("Dockerfile", "blob"),
                    new RepositoryTreeResult("LICENSE", "blob"),
                    new RepositoryTreeResult("src/main/java/Main.java", "blob")
            );

            List<RepositoryTreeResult> result = adapter.filter(tree);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).path()).isEqualTo("src/main/java/Main.java");
        }
    }
}