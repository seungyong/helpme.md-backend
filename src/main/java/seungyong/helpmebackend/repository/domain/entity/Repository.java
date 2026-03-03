package seungyong.helpmebackend.repository.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Repository {
    private String avatarUrl;
    private String name;
    private String owner;
}
