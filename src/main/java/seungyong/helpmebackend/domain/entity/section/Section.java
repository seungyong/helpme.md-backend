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
        String[] splitContents = splitReadmeContent(fullContent, splitMode);
        List<Section> sections = new ArrayList<>();

        for (int i = 0; i < splitContents.length; i++) {
            String content = splitContents[i];
            String title = content.lines()
                    .findFirst()
                    .orElse("Untitled Section")
                    .replaceAll("^(#{1,2} )", "");

            Section section = new Section(
                    null,
                    projectId,
                    title,
                    content.trim(),
                    (short) (i + 1)
            );

            sections.add(section);
        }

        return sections;
    }

    private static String[] splitReadmeContent(String content, SplitMode splitMode) {
        if (splitMode.equals(SplitMode.SPLIT)) {
            return content.split("(?m)(?=^#{1,2} )");
        } else {
            return new String[] { content };
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
}
