package seungyong.helpmebackend.adapter.in.web.dto.repository.response;

import java.util.List;

public record ResponseRepository(
    String owner,
    String name,
    String avatarUrl,
    String defaultBranch
) {

}
