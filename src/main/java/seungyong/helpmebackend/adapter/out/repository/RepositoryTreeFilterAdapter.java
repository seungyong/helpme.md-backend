package seungyong.helpmebackend.adapter.out.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.adapter.out.result.RepositoryTreeResult;
import seungyong.helpmebackend.usecase.port.out.github.repository.RepositoryTreeFilterPortOut;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RepositoryTreeFilterAdapter implements RepositoryTreeFilterPortOut {
    private final Set<String> excludedExtensions;
    private final Set<String> excludedDirectories;
    private final Set<String> excludedFilenames;

    public RepositoryTreeFilterAdapter(
            @Value("${repository.filter.excluded-extensions}") String excludedExtensionsPath,
            @Value("${repository.filter.excluded-directories}") String excludedDirectoriesPath,
            @Value("${repository.filter.excluded-filenames}") String excludedFilenamesPath,
            @Autowired ResourceLoader resourceLoader
    ) throws Exception {
        this.excludedExtensions = loadFile(resourceLoader, excludedExtensionsPath);
        this.excludedDirectories = loadFile(resourceLoader, excludedDirectoriesPath);
        this.excludedFilenames = loadFile(resourceLoader, excludedFilenamesPath);
    }

    @Override
    public List<RepositoryTreeResult> filter(List<RepositoryTreeResult> tree) {
        return tree.stream()
                .filter(item -> item.type().equals("blob"))
                .filter(this::isAnalyzable)
                .toList();
    }

    private Set<String> loadFile(ResourceLoader resourceLoader, String path) throws Exception {
        Resource resource = resourceLoader.getResource(path);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .flatMap(line -> Arrays.stream(line.split(",")))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }
    }

    private boolean isAnalyzable(RepositoryTreeResult item) {
        String path = item.path();

        if (excludedDirectories.stream().anyMatch(dir ->
                path.startsWith(dir + "/") || path.contains("/" + dir + "/"))) {
            return false;
        }

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
                Optional.of(fileName.substring(fileName.lastIndexOf(".")).toLowerCase()) :
                Optional.empty();
    }
}
