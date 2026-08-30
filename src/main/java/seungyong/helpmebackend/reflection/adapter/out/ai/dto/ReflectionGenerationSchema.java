package seungyong.helpmebackend.reflection.adapter.out.ai.dto;

import java.util.List;

public record ReflectionGenerationSchema(String title, List<Section> sections) {
    public record Section(
            String id,
            String title,
            String contentMd,
            List<String> evidenceRefs
    ) {
    }
}
