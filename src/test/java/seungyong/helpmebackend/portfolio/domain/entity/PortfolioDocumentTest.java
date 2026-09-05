package seungyong.helpmebackend.portfolio.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PortfolioDocumentTest {
    @Test
    @DisplayName("지원하지 않는 schema와 비어 있는 section 식별자를 거부")
    void validateDocument() {
        assertThatThrownBy(() -> new PortfolioDocument(2, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PortfolioDocument(1, List.of(
                new PortfolioDocument.Section("", "project_overview", "개요", "내용", List.of())
        ))).isInstanceOf(IllegalArgumentException.class);
    }
}
