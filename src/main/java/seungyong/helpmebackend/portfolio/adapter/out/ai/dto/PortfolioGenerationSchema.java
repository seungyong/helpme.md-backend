package seungyong.helpmebackend.portfolio.adapter.out.ai.dto;

import java.util.List;

public record PortfolioGenerationSchema(List<Section> sections) {
    public record Section(String id, String type, String title, String contentMd, List<String> evidenceRefs) {
    }
}
