package seungyong.helpmebackend.domain.entity.installation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Installation {
    private String installationId;
    private String avatarUrl;
    private String name;
}
