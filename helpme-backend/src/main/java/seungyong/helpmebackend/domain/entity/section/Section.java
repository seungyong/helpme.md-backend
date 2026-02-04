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
}
