package seungyong.helpmebackend.domain.entity.repository;

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
