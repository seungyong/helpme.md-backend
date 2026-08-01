package seungyong.helpmebackend.repository.application;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.global.domain.type.RedisKeyFactory;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.repository.application.dto.ReadmeContext;
import seungyong.helpmebackend.repository.application.port.out.CommitPortOut;
import seungyong.helpmebackend.repository.application.port.out.GPTPortOut;
import seungyong.helpmebackend.repository.application.port.out.ObjectCipherPortOut;
import seungyong.helpmebackend.repository.application.port.out.RepositoryContentPortOut;
import seungyong.helpmebackend.repository.application.port.out.RepositoryQueryPortOut;
import seungyong.helpmebackend.repository.application.port.out.RepositoryTreeFilterPortOut;
import seungyong.helpmebackend.repository.application.port.out.command.RepoBranchCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepoInfoCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepositoryInfoCommand;
import seungyong.helpmebackend.repository.application.port.out.result.CommitResult;
import seungyong.helpmebackend.repository.application.port.out.result.ContributorsResult;
import seungyong.helpmebackend.repository.application.port.out.result.GPTRepositoryInfoResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryFileContentResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryLanguageResult;
import seungyong.helpmebackend.repository.application.port.out.result.RepositoryTreeResult;
import seungyong.helpmebackend.repository.domain.exception.RepositoryErrorCode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
class ReadmeContextLoader {
    private final RepositoryQueryPortOut repositoryQueryPortOut;
    private final RepositoryContentPortOut repositoryContentPortOut;
    private final CommitPortOut commitPortOut;
    private final RepositoryTreeFilterPortOut repositoryTreeFilterPortOut;
    private final GPTPortOut gptPortOut;
    private final RedisPortOut redisPortOut;
    private final ObjectCipherPortOut objectCipherPortOut;

    public ReadmeContext load(
            Long userId,
            String owner,
            String name,
            String accessToken,
            String branch
    ) {
        RepoInfoCommand repository = new RepoInfoCommand(userId, accessToken, owner, name);
        RepoBranchCommand repositoryBranch = new RepoBranchCommand(repository, branch);
        String latestSha = repositoryContentPortOut.getRecentSHA(repositoryBranch);
        if (latestSha == null) {
            throw new CustomException(RepositoryErrorCode.BRANCH_NOT_FOUND);
        }

        Instant expiration = Instant.now().plus(3, ChronoUnit.HOURS);
        String readme = getReadmeWithCache(repositoryBranch, latestSha, expiration);
        List<RepositoryInfoCommand.CommitCommand> commits = getCommitsWithCache(
                repositoryBranch,
                latestSha,
                expiration
        );
        List<RepositoryLanguageResult> languages = getLanguagesWithCache(
                repository,
                latestSha,
                expiration
        );
        List<RepositoryTreeResult> trees = getTreesWithCache(
                repositoryBranch,
                latestSha,
                expiration
        );
        GPTRepositoryInfoResult repositoryInfo = getRepositoryAnalysisWithCache(
                owner,
                name,
                latestSha,
                new RepositoryInfoCommand(languages, commits, trees),
                expiration
        );
        List<RepositoryFileContentResult> entryContents = getFileContentsWithCache(
                "entry files",
                RedisKeyFactory.createEntryFileKey(owner, name, latestSha),
                repositoryBranch,
                repositoryInfo.entryPoints(),
                expiration
        );
        List<RepositoryFileContentResult> importantFileContents = getFileContentsWithCache(
                "important files",
                RedisKeyFactory.createImportanceFileKey(owner, name, latestSha),
                repositoryBranch,
                repositoryInfo.importantFiles(),
                expiration
        );

        return new ReadmeContext(
                readme,
                commits,
                repositoryInfo,
                languages,
                trees,
                entryContents,
                importantFileContents
        );
    }

    private List<RepositoryInfoCommand.CommitCommand> getCommits(RepoBranchCommand command) {
        ContributorsResult contributors = repositoryQueryPortOut.getContributors(command.repoInfo());
        return contributors.contributors().stream()
                .map(contributor -> commitPortOut.getCommits(command, contributor))
                .map(this::toCommitCommand)
                .toList();
    }

    private RepositoryInfoCommand.CommitCommand toCommitCommand(CommitResult result) {
        return new RepositoryInfoCommand.CommitCommand(
                new RepositoryInfoCommand.ContributorCommand(
                        result.contributor().username(),
                        result.contributor().avatarUrl()
                ),
                result.latestCommits().stream().map(CommitResult.Commit::message).toList(),
                result.middleCommits().stream().map(CommitResult.Commit::message).toList(),
                result.initialCommits().stream().map(CommitResult.Commit::message).toList()
        );
    }

    private String getReadmeWithCache(
            RepoBranchCommand command,
            String sha,
            Instant expiration
    ) {
        String key = RedisKeyFactory.createReadmeKey(
                command.repoInfo().owner(),
                command.repoInfo().name(),
                sha
        );
        return getOrLoadAndCache(
                "README",
                key,
                () -> repositoryContentPortOut.getReadmeContent(command),
                redisPortOut::get,
                (writeKey, value) -> redisPortOut.set(writeKey, value, expiration)
        );
    }

    private List<RepositoryInfoCommand.CommitCommand> getCommitsWithCache(
            RepoBranchCommand command,
            String sha,
            Instant expiration
    ) {
        String key = RedisKeyFactory.createCommitsKey(
                command.repoInfo().owner(),
                command.repoInfo().name(),
                sha
        );
        return getOrLoadAndCache(
                "commits",
                key,
                () -> getCommits(command),
                readKey -> redisPortOut.getObject(readKey, new TypeReference<>() {
                }),
                (writeKey, value) -> redisPortOut.setObject(writeKey, value, expiration)
        );
    }

    private List<RepositoryLanguageResult> getLanguagesWithCache(
            RepoInfoCommand command,
            String sha,
            Instant expiration
    ) {
        String key = RedisKeyFactory.createLanguageKey(command.owner(), command.name(), sha);
        return getOrLoadAndCache(
                "languages",
                key,
                () -> repositoryQueryPortOut.getRepositoryLanguages(command),
                readKey -> redisPortOut.getObject(readKey, new TypeReference<>() {
                }),
                (writeKey, value) -> redisPortOut.setObject(writeKey, value, expiration)
        );
    }

    private List<RepositoryTreeResult> getTreesWithCache(
            RepoBranchCommand command,
            String sha,
            Instant expiration
    ) {
        String key = RedisKeyFactory.createTreeKey(
                command.repoInfo().owner(),
                command.repoInfo().name(),
                sha
        );
        return getOrLoadAndCache(
                "repository tree",
                key,
                () -> repositoryTreeFilterPortOut.filter(
                        repositoryContentPortOut.getRepositoryTree(command)
                ),
                readKey -> redisPortOut.getObject(readKey, new TypeReference<>() {
                }),
                (writeKey, value) -> redisPortOut.setObject(writeKey, value, expiration)
        );
    }

    private GPTRepositoryInfoResult getRepositoryAnalysisWithCache(
            String owner,
            String name,
            String sha,
            RepositoryInfoCommand repositoryInfo,
            Instant expiration
    ) {
        String key = RedisKeyFactory.createRepoInfoKey(owner, name, sha);
        return getOrLoadAndCache(
                "repository analysis",
                key,
                () -> gptPortOut.getRepositoryInfo(owner + "/" + name, repositoryInfo),
                readKey -> redisPortOut.getObject(readKey, new TypeReference<>() {
                }),
                (writeKey, value) -> redisPortOut.setObject(writeKey, value, expiration)
        );
    }

    private List<RepositoryFileContentResult> getFileContentsWithCache(
            String cacheName,
            String key,
            RepoBranchCommand command,
            String[] paths,
            Instant expiration
    ) {
        return getOrLoadAndCache(
                cacheName,
                key,
                () -> fetchFileContents(command, getFilePaths(paths)),
                readKey -> objectCipherPortOut.decrypt(
                        redisPortOut.get(readKey),
                        new TypeReference<>() {
                        }
                ),
                (writeKey, value) -> redisPortOut.set(
                        writeKey,
                        objectCipherPortOut.encrypt(value),
                        expiration
                )
        );
    }

    private <T> T getOrLoadAndCache(
            String cacheName,
            String key,
            Supplier<T> loader,
            Function<String, T> cacheReader,
            BiConsumer<String, T> cacheWriter
    ) {
        try {
            T cachedData = cacheReader.apply(key);
            if (cachedData != null) {
                return cachedData;
            }
        } catch (Exception e) {
            log.warn(
                    "README context cache read failed: cache={}, exceptionType={}",
                    cacheName,
                    e.getClass().getSimpleName()
            );
        }

        T data = loader.get();
        if (data != null) {
            try {
                cacheWriter.accept(key, data);
            } catch (Exception e) {
                log.warn(
                        "README context cache write failed: cache={}, exceptionType={}",
                        cacheName,
                        e.getClass().getSimpleName()
                );
            }
        }
        return data;
    }

    private List<String> getFilePaths(String[] paths) {
        if (paths == null || paths.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(paths)
                .filter(path -> path != null && !path.isBlank() && !path.endsWith("/"))
                .toList();
    }

    private List<RepositoryFileContentResult> fetchFileContents(
            RepoBranchCommand command,
            List<String> paths
    ) {
        List<RepositoryFileContentResult> fileContents = new ArrayList<>();
        for (String path : paths) {
            RepositoryFileContentResult content = repositoryContentPortOut.getFileContent(
                    command,
                    new RepositoryTreeResult(path, "blob")
            );
            if (content.content() != null && !content.content().isBlank()) {
                fileContents.add(content);
            }
        }
        return fileContents;
    }
}
