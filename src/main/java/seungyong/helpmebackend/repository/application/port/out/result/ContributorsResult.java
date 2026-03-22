package seungyong.helpmebackend.repository.application.port.out.result;

import java.util.List;

public record ContributorsResult(
        List<Contributor> contributors
) {
    public record Contributor(
            String username,
            String avatarUrl
    ) {
    }
}
