package seungyong.helpmebackend.reflection.domain.entity;

import java.util.List;

public record ReflectionDocument(int schemaVersion, List<Section> sections) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public ReflectionDocument {
        if (schemaVersion != CURRENT_SCHEMA_VERSION || sections == null) {
            throw new IllegalArgumentException("invalid reflection document");
        }
        sections = List.copyOf(sections);
        if (sections.stream().anyMatch(Section::invalid)) {
            throw new IllegalArgumentException("invalid reflection section");
        }
    }

    public static ReflectionDocument empty() {
        return new ReflectionDocument(CURRENT_SCHEMA_VERSION, List.of());
    }

    public String summary() {
        return sections.stream()
                .map(Section::contentMd)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .map(value -> value.length() > 120 ? value.substring(0, 120) : value)
                .orElse(null);
    }

    public record Section(
            String id,
            String type,
            String title,
            String contentMd,
            List<String> evidenceRefs
    ) {
        public Section {
            evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
        }

        private boolean invalid() {
            return id == null || id.isBlank()
                    || !"markdown".equals(type)
                    || title == null || title.isBlank()
                    || contentMd == null
                    || evidenceRefs.stream().anyMatch(ref -> ref == null || ref.isBlank());
        }
    }
}
