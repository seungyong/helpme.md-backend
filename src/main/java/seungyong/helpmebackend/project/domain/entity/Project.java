package seungyong.helpmebackend.project.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Project {
    private Long id;
    private Long userId;
    private String repoFullName;
}
