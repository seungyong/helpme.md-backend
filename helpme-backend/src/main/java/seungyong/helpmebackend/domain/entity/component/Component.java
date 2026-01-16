package seungyong.helpmebackend.domain.entity.component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class Component {
    private Long id;
    private String repoFullName;
    private String title;
    private String content;
    private LocalDateTime updatedAt;
}
