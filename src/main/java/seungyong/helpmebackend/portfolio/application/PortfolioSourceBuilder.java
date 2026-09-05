package seungyong.helpmebackend.portfolio.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.portfolio.application.port.in.command.CustomEvidenceLinkCommand;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioSourcePortOut;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceData;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioSourceSnapshot;
import seungyong.helpmebackend.portfolio.domain.exception.PortfolioErrorCode;
import seungyong.helpmebackend.project.domain.entity.Project;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PortfolioSourceBuilder {
    private final PortfolioSourcePortOut sourcePortOut;

    public PortfolioSourceBuildResult build(Project project, List<Long> reflectionIds, List<Long> activityIds,
                                            List<CustomEvidenceLinkCommand> customLinks) {
        List<Long> normalizedReflections = distinct(reflectionIds);
        List<Long> normalizedActivities = distinct(activityIds);
        if (normalizedReflections.isEmpty()) {
            throw new CustomException(PortfolioErrorCode.PORTFOLIO_SOURCE_REQUIRED);
        }

        PortfolioSourceData selected = sourcePortOut.findSelected(
                project.getId(), normalizedReflections, normalizedActivities
        );
        if (selected.reflections().size() != normalizedReflections.size()) {
            throw new CustomException(PortfolioErrorCode.PORTFOLIO_SOURCE_REQUIRED);
        }
        if (selected.activities().size() != normalizedActivities.size()
                || (!normalizedActivities.isEmpty() && project.isPrivateRepository())) {
            throw new CustomException(PortfolioErrorCode.PORTFOLIO_PRIVATE_EVIDENCE_NOT_ALLOWED);
        }

        List<PortfolioSourceSnapshot.ActivitySource> activities = selected.activities().stream()
                .map(source -> {
                    if (!isSafePublicUrl(source.publicUrl(), project)) {
                        throw new CustomException(PortfolioErrorCode.PORTFOLIO_PRIVATE_EVIDENCE_NOT_ALLOWED);
                    }
                    return new PortfolioSourceSnapshot.ActivitySource(
                            source.id(), source.type(), source.title(), label(source), source.publicUrl()
                    );
                }).toList();
        List<PortfolioSourceSnapshot.CustomLink> links = (customLinks == null ? List.<CustomEvidenceLinkCommand>of() : customLinks)
                .stream().map(link -> validatedLink(link, project)).toList();

        PortfolioSourceSnapshot snapshot = new PortfolioSourceSnapshot(
                selected.reflections().stream().map(source -> new PortfolioSourceSnapshot.ReflectionSource(
                        source.id(), source.kind(), source.periodStart(), source.periodEnd(), source.title(),
                        source.version(), source.content()
                )).toList(),
                activities,
                links
        );
        return new PortfolioSourceBuildResult(snapshot, hash(snapshot));
    }

    public PortfolioSourceBuildResult refresh(Project project, PortfolioSourceSnapshot previous) {
        return build(
                project,
                previous.reflections().stream().map(PortfolioSourceSnapshot.ReflectionSource::id).toList(),
                previous.activities().stream().map(PortfolioSourceSnapshot.ActivitySource::id).toList(),
                previous.customLinks().stream().map(link -> new CustomEvidenceLinkCommand(link.label(), link.url())).toList()
        );
    }

    private List<Long> distinct(List<Long> values) {
        return values == null ? List.of() : values.stream().distinct().sorted().toList();
    }

    private PortfolioSourceSnapshot.CustomLink validatedLink(CustomEvidenceLinkCommand link, Project project) {
        try {
            if (link == null || !StringUtils.hasText(link.label()) || !StringUtils.hasText(link.url())) {
                throw new IllegalArgumentException();
            }
            URI uri = URI.create(link.url());
            String query = uri.getRawQuery() == null ? "" : uri.getRawQuery().toLowerCase();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !StringUtils.hasText(uri.getHost())
                    || hasSecretQuery(query) || isPrivateRepositoryUrl(uri, project)) {
                throw new IllegalArgumentException();
            }
            return new PortfolioSourceSnapshot.CustomLink(link.label().trim(), uri.toString());
        } catch (RuntimeException exception) {
            throw new CustomException(PortfolioErrorCode.PORTFOLIO_PRIVATE_EVIDENCE_NOT_ALLOWED);
        }
    }

    private boolean isSafePublicUrl(String value, Project project) {
        try {
            if (!StringUtils.hasText(value)) return false;

            URI uri = URI.create(value);
            String query = uri.getRawQuery() == null ? "" : uri.getRawQuery().toLowerCase();

            return "https".equalsIgnoreCase(uri.getScheme()) && StringUtils.hasText(uri.getHost())
                    && !hasSecretQuery(query) && !isPrivateRepositoryUrl(uri, project);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * URL 쿼리 문자열에 토큰, 서명, AWS S3 서명과 같은 민감한 정보가 포함되어 있는지 확인합니다.
     * 만약, 쿼리 문자열에 이러한 민감한 정보가 포함되어 있다면, 해당 URL은 안전하지 않은 것으로 간주됩니다.
     */
    private boolean hasSecretQuery(String query) {
        return query.contains("token=") || query.contains("signature=") || query.contains("x-amz-");
    }

    private boolean isPrivateRepositoryUrl(URI uri, Project project) {
        String repositoryPath = "/" + project.getRepoFullName().toLowerCase();
        String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase();

        return project.isPrivateRepository()
                && "github.com".equalsIgnoreCase(uri.getHost())
                && (path.equals(repositoryPath) || path.startsWith(repositoryPath + "/"));
    }

    private String label(PortfolioSourceData.ActivityData activity) {
        String branch = StringUtils.hasText(activity.branchName()) ? activity.branchName() : "activity";
        String sha = StringUtils.hasText(activity.commitSha())
                ? activity.commitSha().substring(0, Math.min(7, activity.commitSha().length())) : null;
        return sha == null ? branch : branch + " · " + sha;
    }

    private String hash(PortfolioSourceSnapshot snapshot) {
        String canonical = snapshot.reflections().stream()
                .sorted(Comparator.comparing(PortfolioSourceSnapshot.ReflectionSource::id))
                .map(item -> "r:%d:%d".formatted(item.id(), item.version()))
                .collect(java.util.stream.Collectors.joining("|"))
                + snapshot.activities().stream()
                .sorted(Comparator.comparing(PortfolioSourceSnapshot.ActivitySource::id))
                .map(item -> "|a:" + item.id()).collect(java.util.stream.Collectors.joining())
                + snapshot.customLinks().stream().sorted(Comparator.comparing(PortfolioSourceSnapshot.CustomLink::url))
                .map(item -> "|l:" + item.label() + ":" + item.url())
                .collect(java.util.stream.Collectors.joining());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("portfolio source hash failed", exception);
        }
    }

}
