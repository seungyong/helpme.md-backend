package seungyong.helpmebackend.section.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
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

    /**
     * README 콘텐츠를 섹션으로 분할하는 정적 팩토리 메서드
     *
     * @param projectId   프로젝트 ID
     * @param fullContent README 전체 콘텐츠
     * @param splitMode   분할 모드 (WHOLE: 전체를 하나의 섹션으로, SPLIT: 제목 기준으로 분할)
     * @return 분할된 섹션 리스트
     */
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

    /**
     * 마크다운 콘텐츠를 코드 블록을 고려하여 안전하게 분할하는 메서드
     * <br />
     * 코드 블록 내의 주석은 분할 기준으로 사용되지 않도록 처리합니다.
     *
     * @param fullMarkdown 전체 마크다운 콘텐츠
     * @return 분할된 섹션 리스트
     */
    private static List<String> splitMarkdownSafely(String fullMarkdown) {
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

    /**
     * 섹션 내용을 업데이트하는 메서드
     *
     * @param content 새로운 내용 (null 또는 빈 문자열인 경우 기존 내용 유지)
     */
    public void updateContent(String content) {
        if (content == null || content.isBlank()) {
            content = "";
        }

        this.content = content;
    }

    /**
     * 섹션 정렬을 업데이트하는 메서드
     *
     * @param orderIdx 새로운 정렬 인덱스
     * @throws IllegalArgumentException orderIdx가 null이거나 음수인 경우 발생
     */
    public void updateOrderIdx(Short orderIdx) {
        if (orderIdx == null) {
            throw new IllegalArgumentException("orderIdx cannot be null");
        } else if (orderIdx < 0) {
            throw new IllegalArgumentException("orderIdx cannot be negative");
        }

        this.orderIdx = orderIdx;
    }
}
