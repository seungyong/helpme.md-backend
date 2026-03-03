package seungyong.helpmebackend.user.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JWTUser {
    private Long id;
    private String username;
}
