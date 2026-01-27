package seungyong.helpmebackend.adapter.out.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.adapter.out.result.RepositoryTreeResult;
import seungyong.helpmebackend.usecase.port.out.github.repository.RepositoryTreeFilterPortOut;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class RepositoryTreeFilterAdapter implements RepositoryTreeFilterPortOut {
    private final Set<String> excludedExtensions;
    private final Set<String> excludedDirectories;
    private final Set<String> excludedFilenames;

    public RepositoryTreeFilterAdapter(
            @Value("${repository.filter.excluded-extensions}") List<String> excludedExtensions,
            @Value("${repository.filter.excluded-directories}") List<String> excludedDirectories,
            @Value("${repository.filter.excluded-filenames}") List<String> excludedFilenames
    ) {
        this.excludedExtensions = new HashSet<>(excludedExtensions);
        this.excludedDirectories = new HashSet<>(excludedDirectories);
        this.excludedFilenames = new HashSet<>(excludedFilenames);
    }

    @Override
    public List<RepositoryTreeResult> filter(List<RepositoryTreeResult> tree) {
        return tree.stream()
                .filter(this::isAnalyzable)
                .toList();
    }

    private boolean isAnalyzable(RepositoryTreeResult item) {
        String path = item.path();

        if (excludedDirectories.stream().anyMatch(dir -> path.contains(dir + "/"))) { return false; }

        String fileName = getFileName(path);
        if (excludedFilenames.contains(fileName)) { return false; }

        Optional<String> extension = getExtension(fileName);
        return extension.filter(s -> !excludedExtensions.contains(s)).isPresent();
    }

    private String getFileName(String path) {
        return path.contains("/") ?
                path.substring(path.lastIndexOf("/") + 1) :
                path;
    }

    private Optional<String> getExtension(String fileName) {
        return fileName.contains(".") ?
                Optional.of(fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase()) :
                Optional.empty();
    }
}
