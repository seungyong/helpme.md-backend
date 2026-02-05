package seungyong.helpmebackend.domain.entity.section;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Section {
    private Long id;
    private Long projectId;
    private String title;
    private String content;
    private Short orderIdx;

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
