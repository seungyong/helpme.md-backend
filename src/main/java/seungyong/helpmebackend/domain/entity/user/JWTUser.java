package seungyong.helpmebackend.domain.entity.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class JWTUser {
    private Long id;
    private String username;
}
