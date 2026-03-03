package seungyong.helpmebackend.repository.application.port.out.result;

public record RepositoryFileContentResult(
        String path,
        String content
) {
}
