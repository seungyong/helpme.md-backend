package seungyong.helpmebackend.adapter.in.web.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.ResponseRepositories;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.ResponseRepository;
import seungyong.helpmebackend.adapter.in.web.dto.user.common.CustomUserDetails;
import seungyong.helpmebackend.usecase.port.in.repository.RepositoryPortIn;

@Tag(
        name = "Repo",
        description = """
                Repo 관련 API
                - 모든 Repo에 대한 정보는 DB에 **저장되지 않습니다.**
                - 각 요청 시점에 `GitHub API`를 호출하여 실시간으로 데이터를 가져옵니다.
                """
)
@RestController
@RequestMapping("/api/v1/repos")
@ResponseBody
@RequiredArgsConstructor
public class RepoController {
    private final RepositoryPortIn repositoryPortIn;

    @GetMapping
    public ResponseEntity<ResponseRepositories> getRepositories(
            @RequestParam("installationId") Long installationId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        return ResponseEntity.ok(
                repositoryPortIn.getRepositories(details.getUserId(), installationId, page)
        );
    }

    @GetMapping("/{owner}/{name}")
    public ResponseEntity<ResponseRepository> getRepository(
            @PathVariable("owner") String owner,
            @PathVariable("name") String name,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        return ResponseEntity.ok(
                repositoryPortIn.getRepository(details.getUserId(), owner, name)
        );
    }
}
