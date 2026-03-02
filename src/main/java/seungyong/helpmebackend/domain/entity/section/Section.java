package seungyong.helpmebackend.domain.entity.section;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Section {
    private Long id;
    private Long projectId;
    private String title;
    private String content;
    private Short orderIdx;

    public enum SplitMode {
        WHOLE,
        SPLIT
    }

    public static List<Section> splitContent(Long projectId, String fullContent, SplitMode splitMode) {
        List<String> splitContents = splitReadmeContent(fullContent, splitMode);
        List<Section> sections = new ArrayList<>();

        for (String content : splitContents) {
            String title = content.lines()
                    .findFirst()
                    .orElse("Untitled Section")
                    .replaceAll("^(#{1,2} )", "")
                    .trim();

            Section section = new Section(
                    null,
                    projectId,
                    title,
                    content.trim(),
                    (short) (sections.size() + 1)
            );

            sections.add(section);
        }

        return sections;
    }

    private static List<String> splitReadmeContent(String content, SplitMode splitMode) {
        if (splitMode.equals(SplitMode.SPLIT)) {
            return splitMarkdownSafely(content);
        } else {
            return List.of(content);
        }
    }

    public void updateContent(String content) {
        if (content == null || content.isBlank()) {
            content = "";
        }

        this.content = content;
    }

    public void updateOrderIdx(Short orderIdx) {
        if (orderIdx == null) {
            throw new IllegalArgumentException("orderIdx cannot be null");
        } else if (orderIdx < 0) {
            throw new IllegalArgumentException("orderIdx cannot be negative");
        }

        this.orderIdx = orderIdx;
    }

    public static List<String> splitMarkdownSafely(String fullMarkdown) {
        List<String> sections = new ArrayList<>();
        StringBuilder currentSection = new StringBuilder();
        boolean inCodeBlock = false;

        String[] lines = fullMarkdown.split("\n");

        for (String line : lines) {
            if (line.trim().startsWith("```")) {
                inCodeBlock = !inCodeBlock;
            }

            if (!inCodeBlock && line.matches("^(#{1,2})\\s+.*")) {
                if (!currentSection.isEmpty()) {
                    sections.add(currentSection.toString().trim());
                    currentSection.setLength(0);
                }
            }

            currentSection.append(line).append("\n");
        }

        if (!currentSection.isEmpty()) {
            sections.add(currentSection.toString().trim());
        }

        return sections;
    }
}
