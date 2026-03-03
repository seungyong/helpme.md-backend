package seungyong.helpmebackend.repository.adapter.in.web.dto.response;

public record ResponseRepository(
    String owner,
    String name,
    String avatarUrl,
    String defaultBranch
) {

}
