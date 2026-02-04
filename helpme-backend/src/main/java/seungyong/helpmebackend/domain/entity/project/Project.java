package seungyong.helpmebackend.domain.entity.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Project {
    private Long id;
    private Long userId;
    private String repoFullName;
}
