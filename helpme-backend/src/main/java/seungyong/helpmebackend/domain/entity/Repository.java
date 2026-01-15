package seungyong.helpmebackend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Repository {
    private String avatarUrl;
    private String name;
    private String owner;
}
