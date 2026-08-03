package seungyong.helpmebackend.github.application.port.in.result;

import seungyong.helpmebackend.github.domain.entity.GithubInstallation;

import java.util.List;

public record GithubInstallationsResult(
        boolean appInstalled,
        List<GithubInstallation> accounts
) {
    public GithubInstallationsResult {
        accounts = List.copyOf(accounts);
    }

    public static GithubInstallationsResult from(List<GithubInstallation> accounts) {
        return new GithubInstallationsResult(!accounts.isEmpty(), accounts);
    }
}
