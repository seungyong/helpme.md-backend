package seungyong.helpmebackend.auth.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Installation {
    private String installationId;
    private String avatarUrl;
    private String name;
}
