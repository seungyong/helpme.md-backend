package seungyong.helpmebackend.domain.entity.component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Component {
    private Long id;
    private Long userId;
    private String repoFullName;
    private String title;
    private String content;

    public static String getFullName(String owner, String name) {
        return owner + "/" + name;
    }
}
