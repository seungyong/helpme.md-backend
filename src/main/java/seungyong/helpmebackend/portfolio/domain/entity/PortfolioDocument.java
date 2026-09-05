package seungyong.helpmebackend.portfolio.domain.entity;

import java.util.List;
import java.util.HashSet;

public record PortfolioDocument(int schemaVersion, List<Section> sections) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public PortfolioDocument {
        if (schemaVersion != CURRENT_SCHEMA_VERSION || sections == null) {
            throw new IllegalArgumentException("invalid portfolio document");
        }
        sections = List.copyOf(sections);
        if (sections.stream().anyMatch(Section::invalid)) {
            throw new IllegalArgumentException("invalid portfolio section");
        }
        if (new HashSet<>(sections.stream().map(Section::id).toList()).size() != sections.size()) {
            throw new IllegalArgumentException("duplicate portfolio section id");
        }
    }

    public static PortfolioDocument empty() {
        return new PortfolioDocument(CURRENT_SCHEMA_VERSION, List.of());
    }

    public record Section(String id, String type, String title, String contentMd, List<String> evidenceRefs) {
        public Section {
            evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
        }

        private boolean invalid() {
            return id == null || id.isBlank() || type == null || type.isBlank()
                    || title == null || title.isBlank() || contentMd == null
                    || evidenceRefs.stream().anyMatch(ref -> ref == null || ref.isBlank());
        }
    }
}
