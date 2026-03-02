package seungyong.helpmebackend.adapter.in.web.dto.repository.response;

public record ResponseRepository(
    String owner,
    String name,
    String avatarUrl,
    String defaultBranch
) {

}
