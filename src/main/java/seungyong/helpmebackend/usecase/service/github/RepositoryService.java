package seungyong.helpmebackend.usecase.service.github;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.adapter.in.web.dto.repository.request.RequestDraftEvaluation;
import seungyong.helpmebackend.adapter.in.web.dto.repository.request.RequestEvaluation;
import seungyong.helpmebackend.adapter.in.web.dto.repository.request.RequestPull;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.*;
import seungyong.helpmebackend.adapter.in.web.dto.section.response.ResponseSections;
import seungyong.helpmebackend.adapter.in.web.mapper.RepositoryPortInMapper;
import seungyong.helpmebackend.adapter.in.web.mapper.SectionPortInMapper;
import seungyong.helpmebackend.adapter.out.command.*;
import seungyong.helpmebackend.adapter.out.result.*;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.common.exception.ErrorCode;
import seungyong.helpmebackend.common.exception.ErrorResponse;
import seungyong.helpmebackend.common.exception.GlobalErrorCode;
import seungyong.helpmebackend.domain.entity.project.Project;
import seungyong.helpmebackend.domain.entity.section.Section;
import seungyong.helpmebackend.domain.entity.user.User;
import seungyong.helpmebackend.domain.exception.RepositoryErrorCode;
import seungyong.helpmebackend.infrastructure.redis.RedisKey;
import seungyong.helpmebackend.infrastructure.redis.RedisKeyFactory;
import seungyong.helpmebackend.infrastructure.sse.SSETaskName;
import seungyong.helpmebackend.usecase.port.in.repository.RepositoryPortIn;
import seungyong.helpmebackend.usecase.port.out.cipher.CipherPortOut;
import seungyong.helpmebackend.usecase.port.out.cipher.ObjectCipherPortOut;
import seungyong.helpmebackend.usecase.port.out.github.repository.CommitPortOut;
import seungyong.helpmebackend.usecase.port.out.github.repository.RepositoryPortOut;
import seungyong.helpmebackend.usecase.port.out.github.repository.RepositoryTreeFilterPortOut;
import seungyong.helpmebackend.usecase.port.out.gpt.GPTPortOut;
import seungyong.helpmebackend.usecase.port.out.project.ProjectPortOut;
import seungyong.helpmebackend.usecase.port.out.redis.RedisPortOut;
import seungyong.helpmebackend.usecase.port.out.section.SectionPortOut;
import seungyong.helpmebackend.usecase.port.out.sse.SSEPortOut;
import seungyong.helpmebackend.usecase.port.out.user.UserPortOut;
import seungyong.helpmebackend.usecase.service.github.dto.ReadmeContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryService implements RepositoryPortIn {
    private final UserPortOut userPortOut;
    private final RepositoryPortOut repositoryPortOut;
    private final CipherPortOut cipherPortOut;
    private final ObjectCipherPortOut objectCipherPortOut;
    private final GPTPortOut gptPortOut;
    private final RedisPortOut redisPortOut;
    private final RepositoryTreeFilterPortOut repositoryTreeFilterPortOut;
    private final SSEPortOut ssePortOut;
    private final ProjectPortOut projectPortOut;
    private final SectionPortOut sectionPortOut;
    private final CommitPortOut commitPortOut;

    @Override
    public ResponseRepositories getRepositories(Long userId, Long installationId, Integer page, Integer perPage) {
        User user = userPortOut.getById(userId);
        String accessToken = cipherPortOut.decrypt(user.getGithubUser().getGithubToken());
        RepositoryResult result = repositoryPortOut.getRepositoriesByInstallationId(accessToken, installationId, page, perPage);
        return new ResponseRepositories(result.repositories(), result.totalCount());
    }

    @Override
    @Transactional
    public ResponseRepository getRepository(Long userId, String owner, String name) {
        User user = userPortOut.getById(userId);
        String accessToken = cipherPortOut.decrypt(user.getGithubUser().getGithubToken());

        RepoInfoCommand repoInfo = new RepoInfoCommand(
                accessToken,
                owner,
                name
        );

        // Repository 정보 조회
        RepositoryDetailResult repository = repositoryPortOut.getRepository(repoInfo);

        return RepositoryPortInMapper.INSTANCE.toResponseRepository(repository);
    }

    @Override
    public ResponseBranches getBranches(Long userId, String owner, String name) {
        User user = userPortOut.getById(userId);
        String accessToken = cipherPortOut.decrypt(user.getGithubUser().getGithubToken());

        RepoInfoCommand repoInfo = new RepoInfoCommand(
                accessToken,
                owner,
                name
        );

        List<String> branches = repositoryPortOut.getAllBranches(repoInfo);
        return new ResponseBranches(branches);
    }

    @Override
    public ResponseEvaluation fallbackDraftEvaluation(String taskId) {
        return getFallbackResult(
                RedisKey.SSE_EMITTER_EVALUATION_DRAFT_KEY.getValue() + taskId,
                taskId,
                new TypeReference<ResponseEvaluation>() {}
        );
    }

    @Override
    public ResponseSections fallbackGenerateReadme(String taskId) {
        return getFallbackResult(
                RedisKey.SSE_EMITTER_GENERATION_KEY.getValue() + taskId,
                taskId,
                new TypeReference<ResponseSections>() {}
        );
    }

    @Override
    public ResponsePull createPullRequest(RequestPull request, Long userId, String owner, String name) {
        User user = userPortOut.getById(userId);
        String accessToken = cipherPortOut.decrypt(user.getGithubUser().getGithubToken());

        RepoInfoCommand repoInfo = new RepoInfoCommand(
                accessToken,
                owner,
                name
        );

        RepoBranchCommand branchCommand = new RepoBranchCommand(
                repoInfo,
                request.branch()
        );

        // 1. 최신 커밋 SHA 조회
        String recentSHA = repositoryPortOut.getRecentSHA(branchCommand);

        // 2. 새로운 브랜치 생성 (Readme 수정 내용 반영용)
        String newBranchName = "readme-proposals/" + UUID.randomUUID();
        repositoryPortOut.createBranch(
                new CreateBranchCommand(
                        repoInfo,
                        newBranchName,
                        recentSHA
                )
        );

        try {
            // 3. README.md 파일의 SHA 조회
            String readmeSHA = repositoryPortOut.getReadmeSHA(branchCommand);

            // 4. README.md 파일 수정 푸시
            String commitMessage = "Update README.md via HelpMe";
            repositoryPortOut.push(
                    new ReadmePushCommand(
                            repoInfo,
                            newBranchName,
                            request.content(),
                            readmeSHA,
                            commitMessage
                    )
            );

            // 5. Pull Request 생성
            String prTitle = "[HelpMe] Improve README.md";
            String prBody = "This pull request is created automatically by HelpMe to improve the README.md file.";
            String prUrl = repositoryPortOut.createPullRequest(
                    new CreatePullRequestCommand(
                            repoInfo,
                            newBranchName,
                            request.branch(),
                            prTitle,
                            prBody
                    )
            );

            return new ResponsePull(prUrl);
        } catch (Exception e) {
            log.error("Deleting branch due to error during pull request creation: {}", newBranchName, e);

            // 에러 발생 시 생성한 브랜치 삭제
            repositoryPortOut.deleteBranch(
                    new RepoBranchCommand(
                            repoInfo,
                            newBranchName
                    )
            );

            throw e;
        }
    }

    @Async
    @Override
    public void evaluateDraftReadme(RequestDraftEvaluation request, String taskId, Long userId, String owner, String name) {
        try {
            User user = userPortOut.getById(userId);
            String accessToken = cipherPortOut.decrypt(user.getGithubUser().getGithubToken());

            ReadmeContext readmeContext = generateReadmeContext(
                    owner,
                    name,
                    accessToken,
                    request.branch()
            );

            EvaluationContentResult response = gptPortOut.evaluateReadme(
                    new EvaluationCommand(
                            owner + "/" + name,
                            request.content(),
                            new RepositoryInfoCommand(
                                    readmeContext.languages(),
                                    readmeContext.commits(),
                                    readmeContext.trees()
                            ),
                            readmeContext.entryContents(),
                            readmeContext.importantFileContents(),
                            readmeContext.repositoryInfo().techStack(),
                            readmeContext.repositoryInfo().projectSize()
                    )
            );

            sseSend(
                    RedisKey.SSE_EMITTER_EVALUATION_DRAFT_KEY.getValue() + taskId,
                    taskId,
                    SSETaskName.COMPLETION_EVALUATE_DRAFT.getTaskName(),
                    new ResponseEvaluation(
                            response.rating(),
                            response.contents()
                    )
            );
        } catch (Exception e) {
            sseSendError(
                    taskId,
                    SSETaskName.COMPLETION_EVALUATE_DRAFT_ERROR.getTaskName(),
                    e
            );
        }
    }

    @Async
    @Transactional
    @Override
    public void generateDraftReadme(RequestEvaluation request, String taskId, Long userId, String owner, String name) {
        try {
            User user = userPortOut.getById(userId);
            String accessToken = cipherPortOut.decrypt(user.getGithubUser().getGithubToken());

            ReadmeContext readmeContext = generateReadmeContext(
                    owner,
                    name,
                    accessToken,
                    request.branch()
            );

            String draftReadme = gptPortOut.generateDraftReadme(
                    new GenerateReadmeCommand(
                            owner + "/" + name,
                            readmeContext.readme(),
                            new RepositoryInfoCommand(
                                    readmeContext.languages(),
                                    readmeContext.commits(),
                                    readmeContext.trees()
                            ),
                            readmeContext.entryContents(),
                            readmeContext.importantFileContents(),
                            readmeContext.repositoryInfo().techStack(),
                            readmeContext.repositoryInfo().projectSize()
                    )
            );

            Project project = projectPortOut.getByUserIdAndRepoFullName(userId, owner + "/" + name)
                    .orElseGet(() -> projectPortOut.save(new Project(null, userId, owner + "/" + name)));

            List<Section> existingSections = sectionPortOut.getSectionsByUserIdAndRepoFullName(userId, owner + "/" + name);

            if (!existingSections.isEmpty()) {
                sectionPortOut.deleteAllByUserIdAndRepoFullName(userId, owner + "/" + name);
            }

            List<Section> savedSections = sectionPortOut.saveAll(
                    Section.splitContent(
                            project.getId(),
                            draftReadme,
                            Section.SplitMode.SPLIT
                    )
            );

            sseSend(
                    RedisKey.SSE_EMITTER_GENERATION_KEY.getValue() + taskId,
                    taskId,
                    SSETaskName.COMPLETION_GENERATE.getTaskName(),
                    new ResponseSections(
                            savedSections.stream()
                                    .map(SectionPortInMapper.INSTANCE::toResponseSection)
                                    .toList()
                    )
            );
        } catch (Exception e) {
            sseSendError(
                    taskId,
                    SSETaskName.COMPLETION_GENERATE_ERROR.getTaskName(),
                    e
            );
        }
    }

    private <T> T getFallbackResult(String key, String taskId, TypeReference<T> typeReference) {
        T cached = redisPortOut.getObject(key, typeReference);

        if (cached == null) {
            throw new CustomException(RepositoryErrorCode.FALLBACK_NOT_FOUND);
        }

        ssePortOut.deleteEmitter(taskId);
        redisPortOut.delete(key);
        return cached;
    }

    private void sseSend(String key, String taskId, String taskName, Object data) {
        boolean isSent = ssePortOut.sendCompletion(taskId, taskName, data);

        if (!isSent) {
            redisPortOut.setObjectIfAbsent(
                    key,
                    data,
                    Instant.now().plus(1, ChronoUnit.HOURS)
            );
        }
    }

    private void sseSendError(String taskId, String taskName, Exception e) {
        log.error("SSE send error for task {}: {}", taskId, e.getMessage(), e);

        if (e instanceof CustomException) {
            ssePortOut.sendCompletion(
                    taskId,
                    taskName,
                    ErrorResponse.toResponseEntity((ErrorCode) ((CustomException) e).getErrorCode())
            );
        } else {
            ssePortOut.sendCompletion(
                    taskId,
                    taskName,
                    new CustomException(GlobalErrorCode.INTERNAL_SERVER_ERROR)
            );
        }
    }

    private ReadmeContext generateReadmeContext(
            String owner,
            String name,
            String accessToken,
            String branch
    ) {
        RepoInfoCommand repoInfoCommand = new RepoInfoCommand(
                accessToken,
                owner,
                name
        );

        RepoBranchCommand branchCommand = new RepoBranchCommand(
                repoInfoCommand,
                branch
        );

        // 최신 커밋 SHA 조회 (캐싱 여부 판단용)
        String latestShaKey = repositoryPortOut.getRecentSHA(branchCommand);

        String readme;
        GPTRepositoryInfoResult repositoryInfo;
        List<RepositoryInfoCommand.CommitCommand> commits;
        List<RepositoryLanguageResult> languages;
        List<RepositoryTreeResult> trees;
        List<RepositoryFileContentResult> entryContents;
        List<RepositoryFileContentResult> importantFileContents;

        if (latestShaKey != null) {
            Instant expiration = Instant.now().plus(3, ChronoUnit.HOURS);

            readme = getReadmeWithCache(branchCommand, latestShaKey, expiration);
            commits = getCommitsWithCache(
                    branchCommand,
                    latestShaKey,
                    expiration
            );
            languages = getLanguagesWithCache(repoInfoCommand, latestShaKey, expiration);
            trees = getTreesWithCache(branchCommand, latestShaKey, expiration);
            repositoryInfo = getRepositoryWithCache(
                    owner, name, latestShaKey,
                    new RepositoryInfoCommand(
                            languages,
                            commits,
                            trees
                    ),
                    expiration
            );
            entryContents = getEntryContentsWithCache(branchCommand, repositoryInfo, latestShaKey, expiration);
            importantFileContents = getImportantFileContentsWithCache(
                    branchCommand,
                    repositoryInfo,
                    latestShaKey, expiration
            );
        } else {
            readme = repositoryPortOut.getReadmeContent(branchCommand);
            commits = getCommits(branchCommand);
            languages = repositoryPortOut.getRepositoryLanguages(repoInfoCommand);
            trees = repositoryTreeFilterPortOut.filter(
                    repositoryPortOut.getRepositoryTree(branchCommand)
            );

            repositoryInfo = gptPortOut.getRepositoryInfo(
                    owner + "/" + name,
                    new RepositoryInfoCommand(
                            languages,
                            commits,
                            trees
                    )
            );

            entryContents = fetchFileContents(
                    branchCommand,
                    getFilePaths(repositoryInfo.entryPoints())
            );

            importantFileContents = fetchFileContents(
                    branchCommand,
                    getFilePaths(repositoryInfo.importantFiles())
            );
        }

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

    private <T> T getOrLoadAndCache(
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
            log.warn("Cache read failed for key {}: {}", key, e.getMessage());
        }

        T data = loader.get();

        if (data != null) {
            try {
                cacheWriter.accept(key, data);
            } catch (Exception e) {
                log.warn("Cache write failed for key {}: {}", key, e.getMessage());
            }
        }

        return data;
    }

    private List<RepositoryInfoCommand.CommitCommand> getCommits(
            RepoBranchCommand command
    ) {
        ContributorsResult contributors = repositoryPortOut.getContributors(command.repoInfo());
        List<CompletableFuture<CommitResult>> commitFutures = contributors.contributors().stream()
                .map(contributor -> CompletableFuture.supplyAsync(() ->
                        commitPortOut.getCommits(command, contributor)
                ))
                .toList();

        // size 계산보다 자바가 알아서 크기 최적화하는 것이 빠르므로 toArray에 0 전달
        CompletableFuture<List<CommitResult>> commitsFuture = CompletableFuture.allOf(commitFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> commitFutures.stream()
                        .map(CompletableFuture::join)
                        .toList()
                );

        return commitsFuture.join().stream()
                .map(commitResult -> new RepositoryInfoCommand.CommitCommand(
                        new RepositoryInfoCommand.ContributorCommand(
                                commitResult.contributor().username(),
                                commitResult.contributor().avatarUrl()
                        ),
                        commitResult.latestCommits().stream()
                                .map(CommitResult.Commit::message)
                                .toList(),
                        commitResult.middleCommits().stream()
                                .map(CommitResult.Commit::message)
                                .toList(),
                        commitResult.initialCommits().stream()
                                .map(CommitResult.Commit::message)
                                .toList()
                ))
                .toList();
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
                key,
                () -> repositoryPortOut.getReadmeContent(command),
                redisPortOut::get,
                (writeKey, val) -> redisPortOut.set(writeKey, val, expiration)
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
                key,
                () -> getCommits(command),
                (readKey) -> redisPortOut.getObject(readKey, new TypeReference<List<RepositoryInfoCommand.CommitCommand>>() {}),
                (writeKey, val) -> redisPortOut.setObject(writeKey, val, expiration)
        );
    }

    private List<RepositoryLanguageResult> getLanguagesWithCache(
            RepoInfoCommand command,
            String sha,
            Instant expiration
    ) {
        String key = RedisKeyFactory.createLanguageKey(
                command.owner(),
                command.name(),
                sha
        );

        return getOrLoadAndCache(
                key,
                () -> repositoryPortOut.getRepositoryLanguages(command),
                (readKey) -> redisPortOut.getObject(readKey, new TypeReference<List<RepositoryLanguageResult>>() {}),
                (writeKey, val) -> redisPortOut.setObject(writeKey, val, expiration)
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
                key,
                () -> {
                    List<RepositoryTreeResult> results = repositoryPortOut.getRepositoryTree(command);
                    return repositoryTreeFilterPortOut.filter(results);
                },
                (readKey) -> redisPortOut.getObject(readKey, new TypeReference<List<RepositoryTreeResult>>() {}),
                (writeKey, val) -> redisPortOut.setObject(writeKey, val, expiration)
        );
    }

    private GPTRepositoryInfoResult getRepositoryWithCache(
            String owner,
            String name,
            String sha,
            RepositoryInfoCommand repositoryInfo,
            Instant expiration
    ) {
        String key = RedisKeyFactory.createRepoInfoKey(owner, name, sha);

        return getOrLoadAndCache(
                key,
                () -> gptPortOut.getRepositoryInfo(
                        owner + "/" + name,
                        repositoryInfo
                ),
                (readKey) -> redisPortOut.getObject(readKey, new TypeReference<GPTRepositoryInfoResult>() {}),
                (writeKey, val) -> redisPortOut.setObject(writeKey, val, expiration)
        );
    }

    private List<RepositoryFileContentResult> getEntryContentsWithCache(
            RepoBranchCommand command,
            GPTRepositoryInfoResult repositoryInfo,
            String sha,
            Instant expiration
    ) {
        String key = RedisKeyFactory.createEntryFileKey(
                command.repoInfo().owner(),
                command.repoInfo().name(),
                sha
        );

        return getOrLoadAndCache(
                key,
                () -> fetchFileContents(
                        command,
                        getFilePaths(repositoryInfo.entryPoints())
                ),
                (readKey) -> {
                    String encrypted = redisPortOut.get(readKey);
                    return objectCipherPortOut.decrypt(
                            encrypted,
                            new TypeReference<List<RepositoryFileContentResult>>() {}
                    );
                },
                (writeKey, val) -> {
                    String encrypted = objectCipherPortOut.encrypt(val);
                    redisPortOut.set(writeKey, encrypted, expiration);
                }
        );
    }

    private List<RepositoryFileContentResult> getImportantFileContentsWithCache(
            RepoBranchCommand command,
            GPTRepositoryInfoResult repositoryInfo,
            String sha,
            Instant expiration
    ) {
        String key = RedisKeyFactory.createImportanceFileKey(
                command.repoInfo().owner(),
                command.repoInfo().name(),
                sha
        );

        return getOrLoadAndCache(
                key,
                () -> fetchFileContents(
                        command,
                        getFilePaths(repositoryInfo.importantFiles())
                ),
                (readKey) -> {
                    String encrypted = redisPortOut.get(readKey);
                    return objectCipherPortOut.decrypt(
                            encrypted,
                            new TypeReference<List<RepositoryFileContentResult>>() {}
                    );
                },
                (writeKey, val) -> {
                    String encrypted = objectCipherPortOut.encrypt(val);
                    redisPortOut.set(writeKey, encrypted, expiration);
                }
        );
    }

    private List<String> getFilePaths(String[] paths) {
        if (paths == null || paths.length == 0) {
            return Collections.emptyList();
        }

        return Arrays.stream(paths)
                // 끝이 / 로 끝나는 경로는 디렉토리이므로 제외 (GPT 응답이 항상 정확하지 않을 수 있으므로 방어적 코딩)
                .filter(path -> path != null && !path.isBlank() && !path.endsWith("/"))
                .toList();
    }

    private List<RepositoryFileContentResult> fetchFileContents(
            RepoBranchCommand command,
            List<String> paths
    ) {
        List<RepositoryFileContentResult> fileContents = new ArrayList<>();

        for (String path : paths) {
            RepositoryFileContentResult contentResult = repositoryPortOut.getFileContent(
                    command,
                    new RepositoryTreeResult(
                            path,
                            "file"
                    )
            );

            if (contentResult.content() == null || contentResult.content().isBlank()) {
                continue;
            }

            fileContents.add(contentResult);
        }

        return fileContents;
    }
}
