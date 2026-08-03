package seungyong.helpmebackend.github.adapter.in.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import seungyong.helpmebackend.github.application.port.in.result.GithubInstallationsResult;
import seungyong.helpmebackend.github.domain.entity.GithubInstallation;

import java.util.List;

@Schema(description = "GitHub App 설치 계정 목록")
public record ResponseGithubInstallations(
        boolean appInstalled,
        List<Account> accounts
) {
    public static ResponseGithubInstallations from(GithubInstallationsResult result) {
        return new ResponseGithubInstallations(
                result.appInstalled(),
                result.accounts().stream().map(Account::from).toList()
        );
    }

    public record Account(
            long installationId,
            String login,
            String type,
            String repositorySelection,
            int repositoryCount
    ) {
        private static Account from(GithubInstallation installation) {
            return new Account(
                    installation.installationId(),
                    installation.login(),
                    installation.type().getApiValue(),
                    installation.repositorySelection().getApiValue(),
                    installation.repositoryCount()
            );
        }
    }
}
