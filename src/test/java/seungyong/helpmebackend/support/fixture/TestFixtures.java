package seungyong.helpmebackend.support.fixture;

import seungyong.helpmebackend.auth.adapter.in.web.dto.response.ResponseInstallations;
import seungyong.helpmebackend.auth.application.port.out.result.OAuthGithubUser;
import seungyong.helpmebackend.auth.application.port.out.result.OAuthTokenResult;
import seungyong.helpmebackend.auth.domain.entity.Installation;
import seungyong.helpmebackend.global.domain.entity.JWT;
import seungyong.helpmebackend.project.adapter.out.persistence.entity.ProjectJpaEntity;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.repository.adapter.in.web.dto.request.RequestDraftEvaluation;
import seungyong.helpmebackend.repository.adapter.in.web.dto.request.RequestGeneration;
import seungyong.helpmebackend.repository.adapter.in.web.dto.request.RequestPull;
import seungyong.helpmebackend.repository.adapter.in.web.dto.response.ResponseEvaluation;
import seungyong.helpmebackend.repository.application.port.out.command.CreateBranchCommand;
import seungyong.helpmebackend.repository.application.port.out.command.CreatePullRequestCommand;
import seungyong.helpmebackend.repository.application.port.out.command.EvaluationCommand;
import seungyong.helpmebackend.repository.application.port.out.command.GenerateReadmeCommand;
import seungyong.helpmebackend.repository.application.port.out.command.ReadmePushCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepoBranchCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepoInfoCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepoPermissionCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepositoryInfoCommand;
import seungyong.helpmebackend.repository.application.port.out.result.ContributorsResult;
import seungyong.helpmebackend.repository.application.port.out.result.EvaluationContentResult;
import seungyong.helpmebackend.repository.application.port.out.result.GPTRepositoryInfoResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryDetailResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryFileContentResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryLanguageResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryTreeResult;
import seungyong.helpmebackend.repository.domain.entity.EncryptedToken;
import seungyong.helpmebackend.repository.domain.entity.Repository;
import seungyong.helpmebackend.section.adapter.in.web.dto.request.RequestSection;
import seungyong.helpmebackend.section.adapter.in.web.dto.response.ResponseSections;
import seungyong.helpmebackend.section.domain.entity.Section;
import seungyong.helpmebackend.user.adapter.out.persistence.entity.UserJpaEntity;
import seungyong.helpmebackend.user.domain.entity.GithubUser;
import seungyong.helpmebackend.user.domain.entity.User;
import seungyong.helpmebackend.user.domain.type.GithubTokenStatus;
import seungyong.helpmebackend.user.domain.type.UserStatus;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 테스트의 전제와 실패 원인을 쉽게 읽을 수 있도록 고정값만 제공하는 fixture 모음입니다.
 * 계약에 중요한 값은 각 테스트에서 생성자나 전용 factory 인자로 명시합니다.
 */
public final class TestFixtures {

    private static final OffsetDateTime AUTHENTICATED_AT =
            OffsetDateTime.of(2026, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC);
    private static final Instant TOKEN_EXPIRES_AT = Instant.parse("2030-01-01T00:00:00Z");

    private TestFixtures() {
    }

    public static EncryptedToken encryptedToken() {
        return new EncryptedToken("encrypted-github-token");
    }

    public static GithubUser githubUser() {
        return githubUser(1001L, "encrypted-github-token");
    }

    public static GithubUser githubUser(Long githubId, String token) {
        return GithubUser.authenticated(
                "test-user",
                githubId,
                new EncryptedToken(token),
                AUTHENTICATED_AT
        );
    }

    public static User user() {
        return user(null);
    }

    public static User user(Long id) {
        return User.builder()
                .id(id)
                .githubUser(githubUser())
                .status(UserStatus.ACTIVE)
                .build();
    }

    public static User user(Long id, String token) {
        return User.builder()
                .id(id)
                .githubUser(githubUser(1001L, token))
                .status(UserStatus.ACTIVE)
                .build();
    }

    public static UserJpaEntity userJpaEntity() {
        return userJpaEntity(null, 1001L, "encrypted-github-token");
    }

    public static UserJpaEntity userJpaEntity(Long id, Long githubId, String token) {
        return UserJpaEntity.builder()
                .id(id)
                .name("test-user")
                .githubId(githubId)
                .githubToken(new EncryptedToken(token))
                .githubTokenStatus(GithubTokenStatus.VALID)
                .status(UserStatus.ACTIVE)
                .build();
    }

    public static JWT jwt() {
        return jwt("access-token", "refresh-token");
    }

    public static JWT jwt(String accessToken, String refreshToken) {
        return JWT.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .accessTokenExpireTime(TOKEN_EXPIRES_AT)
                .refreshToken(refreshToken)
                .refreshTokenExpireTime(TOKEN_EXPIRES_AT)
                .build();
    }

    public static OAuthTokenResult oauthTokenResult() {
        return new OAuthTokenResult("github-access-token", "bearer", "read:user");
    }

    public static OAuthGithubUser oauthGithubUser() {
        return new OAuthGithubUser("octocat", 1001L);
    }

    public static Installation installation() {
        return new Installation("101", "https://example.com/avatar.png", "octocat/helpme-md");
    }

    public static List<Installation> installations() {
        return List.of(
                installation(),
                new Installation("102", "https://example.com/avatar-2.png", "octocat/helpme-md-api")
        );
    }

    public static ResponseInstallations responseInstallations() {
        return new ResponseInstallations(installations());
    }

    public static Project project(Long userId) {
        return project(null, userId, "octocat/helpme-md");
    }

    public static Project project(Long id, Long userId, String repoFullName) {
        return new Project(id, userId, repoFullName);
    }

    public static ProjectJpaEntity projectJpaEntity(UserJpaEntity user) {
        return ProjectJpaEntity.builder()
                .user(user)
                .repoFullName("octocat/helpme-md")
                .build();
    }

    public static Section section(Long projectId) {
        return section(null, projectId, 1);
    }

    public static Section section(Long id, Long projectId, int orderIdx) {
        return new Section(id, projectId, "프로젝트 소개", "Helpme.md 프로젝트입니다.", orderIdx);
    }

    public static RequestSection requestSection() {
        return new RequestSection("프로젝트 소개", "Helpme.md 프로젝트입니다.");
    }

    public static ResponseSections.Section responseSection() {
        return new ResponseSections.Section(1L, "프로젝트 소개", "Helpme.md 프로젝트입니다.", 1);
    }

    public static ResponseSections responseSections() {
        return new ResponseSections(List.of(responseSection()));
    }

    public static Repository repository() {
        return new Repository("https://example.com/avatar.png", "helpme-md", "octocat");
    }

    public static RepositoryResult repositoryResult() {
        return new RepositoryResult(List.of(repository()), 1);
    }

    public static RepositoryDetailResult repositoryDetailResult() {
        return new RepositoryDetailResult(
                "https://example.com/avatar.png",
                "octocat",
                "helpme-md",
                "main"
        );
    }

    public static GPTRepositoryInfoResult gptRepositoryInfoResult() {
        return new GPTRepositoryInfoResult(
                new String[]{"Java", "Spring Boot"},
                "medium",
                new String[]{"src/main/java/Application.java"},
                new String[]{"build.gradle"}
        );
    }

    public static EvaluationContentResult evaluationContentResult() {
        return new EvaluationContentResult(4.5f, List.of("프로젝트 설명이 명확합니다."));
    }

    public static ResponseEvaluation responseEvaluation() {
        return new ResponseEvaluation(4.5f, List.of("프로젝트 설명이 명확합니다."));
    }

    public static RepoInfoCommand repoInfoCommand() {
        return new RepoInfoCommand(1L, "github-access-token", "octocat", "helpme-md");
    }

    public static RepoBranchCommand repoBranchCommand() {
        return new RepoBranchCommand(repoInfoCommand(), "main");
    }

    public static CreateBranchCommand createBranchCommand() {
        return new CreateBranchCommand(repoInfoCommand(), "helpme/readme", "base-commit-sha");
    }

    public static ReadmePushCommand readmePushCommand() {
        return readmePushCommand("readme-blob-sha");
    }

    public static ReadmePushCommand readmePushCommand(String readmeSha) {
        return new ReadmePushCommand(
                repoInfoCommand(),
                "helpme/readme",
                "# Helpme.md",
                readmeSha,
                "docs: update README"
        );
    }

    public static CreatePullRequestCommand createPullRequestCommand() {
        return new CreatePullRequestCommand(
                repoInfoCommand(),
                "helpme/readme",
                "main",
                "docs: update README",
                "README를 갱신합니다."
        );
    }

    public static RepoPermissionCommand repoPermissionCommand() {
        return new RepoPermissionCommand(repoInfoCommand(), "octocat");
    }

    public static ContributorsResult.Contributor contributor() {
        return new ContributorsResult.Contributor(
                "octocat",
                "https://example.com/avatar.png"
        );
    }

    public static RepositoryInfoCommand repositoryInfoCommand() {
        RepositoryInfoCommand.ContributorCommand contributor =
                new RepositoryInfoCommand.ContributorCommand(
                        "octocat",
                        "https://example.com/avatar.png"
                );
        RepositoryInfoCommand.CommitCommand commit = new RepositoryInfoCommand.CommitCommand(
                contributor,
                List.of("최근 커밋"),
                List.of("중간 커밋"),
                List.of("초기 커밋")
        );

        return new RepositoryInfoCommand(
                List.of(new RepositoryLanguageResult("Java", 1024L)),
                List.of(commit),
                List.of(new RepositoryTreeResult("src/main/java/Application.java", "blob"))
        );
    }

    public static EvaluationCommand evaluationCommand() {
        return new EvaluationCommand(
                "octocat/helpme-md",
                "# Helpme.md",
                repositoryInfoCommand(),
                List.of(new RepositoryFileContentResult("src/main/java/Application.java", "class Application {}")),
                List.of(new RepositoryFileContentResult("build.gradle", "plugins {}")),
                new String[]{"Java", "Spring Boot"},
                "medium"
        );
    }

    public static GenerateReadmeCommand generateReadmeCommand() {
        return new GenerateReadmeCommand(
                "octocat/helpme-md",
                "# Existing README",
                repositoryInfoCommand(),
                List.of(new RepositoryFileContentResult("src/main/java/Application.java", "class Application {}")),
                List.of(new RepositoryFileContentResult("build.gradle", "plugins {}")),
                new String[]{"Java", "Spring Boot"},
                "medium"
        );
    }

    public static RequestPull requestPull() {
        return new RequestPull("helpme/readme", "# Helpme.md");
    }

    public static RequestDraftEvaluation requestDraftEvaluation() {
        return new RequestDraftEvaluation("main", "# Helpme.md");
    }

    public static RequestGeneration requestGeneration() {
        return new RequestGeneration("main");
    }
}
