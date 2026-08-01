package seungyong.helpmebackend.repository.application.port.in.result;

import java.util.List;

public record GeneratedReadmeResult(
        List<Section> sections
) {
    public record Section(
            Long id,
            String title,
            String content,
            Integer orderIdx
    ) {
    }
}
