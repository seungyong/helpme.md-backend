package seungyong.helpmebackend.usecase.service.github;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.adapter.in.web.dto.repository.request.RequestDraftEvaluation;
import seungyong.helpmebackend.adapter.in.web.dto.repository.request.RequestEvaluation;
import seungyong.helpmebackend.adapter.in.web.dto.repository.request.RequestPull;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.*;
import seungyong.helpmebackend.adapter.in.web.mapper.RepositoryPortInMapper;
import seungyong.helpmebackend.adapter.out.command.*;
import seungyong.helpmebackend.adapter.out.result.*;
import seungyong.helpmebackend.domain.entity.component.Component;
import seungyong.helpmebackend.domain.entity.evaluation.Evaluation;
import seungyong.helpmebackend.domain.entity.user.User;
import seungyong.helpmebackend.domain.mapper.CustomTimeStamp;
import seungyong.helpmebackend.domain.vo.EvaluationStatus;
import seungyong.helpmebackend.infrastructure.gpt.dto.EvaluationContent;
import seungyong.helpmebackend.infrastructure.gpt.dto.GPTRepositoryInfo;
import seungyong.helpmebackend.infrastructure.redis.RedisKeyFactory;
import seungyong.helpmebackend.usecase.port.in.repository.RepositoryPortIn;
import seungyong.helpmebackend.usecase.port.out.cipher.CipherPortOut;
import seungyong.helpmebackend.usecase.port.out.component.ComponentPortOut;
import seungyong.helpmebackend.usecase.port.out.evaluation.EvaluationPortOut;
import seungyong.helpmebackend.usecase.port.out.github.repository.RepositoryPortOut;
import seungyong.helpmebackend.usecase.port.out.gpt.GPTPortOut;
import seungyong.helpmebackend.usecase.port.out.redis.RedisPortOut;
import seungyong.helpmebackend.usecase.port.out.user.UserPortOut;
import seungyong.helpmebackend.usecase.service.github.dto.ReadmeContext;
import seungyong.helpmebackend.usecase.service.github.helper.CacheLoader;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryService implements RepositoryPortIn {
    private final UserPortOut userPortOut;
    private final RepositoryPortOut repositoryPortOut;
    private final EvaluationPortOut evaluationPortOut;
    private final ComponentPortOut componentPortOut;
    private final CipherPortOut cipherPortOut;
    private final GPTPortOut gptPortOut;
    private final RedisPortOut redisPortOut;

    @Override
    public ResponseRepositories getRepositories(Long userId, Long installationId, Integer page) {
        User user = userPortOut.getById(userId);
        String accessToken = cipherPortOut.decrypt(user.getGithubUser().getGithubToken());
        RepositoryResult result = repositoryPortOut.getRepositoriesByInstallationId(accessToken, installationId, page);
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
        String fullName = owner + "/" + name;

        // ReadME.md 내용 조회
        String content = repositoryPortOut.getReadmeContent(
                new RepoBranchCommand(
                        repoInfo,
                        repository.defaultBranch()
                )
        );

        // Evaluation 정보 조회 및 없으면 생성
        Evaluation evaluation = evaluationPortOut.getByFullName(fullName)
                .orElse(null);

        /*
        * 1. Evaluation이 없고, ReadME.md 내용이 비어있는 경우 -> NONE 상태의 Evaluation 생성
        * 2. Evaluation이 없고, ReadME.md 내용이 존재하는 경우 -> CREATED 상태의 Evaluation 생성
        * 3. Evaluation이 존재하는데, ReadME.md 내용이 비어있는 경우 -> NONE 상태로 변경
        * 4. Evaluation이 NONE 상태인데, ReadME.md 내용이 존재하는 경우 -> CREATED 상태로 변경
        * */
        if (evaluation == null && content.isBlank()) {
            evaluation = Evaluation.createNoneStatusEvaluation(user.getId(), fullName);
            evaluation = evaluationPortOut.save(evaluation);
        } else if (evaluation == null) {
            evaluation = Evaluation.createWithStatusEvaluation(
                    user.getId(),
                    fullName,
                    null,
                    null
            );
            evaluation = evaluationPortOut.save(evaluation);
        } else if (!evaluation.getStatus().equals(EvaluationStatus.NONE) && content.isBlank()) {
            evaluation.changeNoneStatus();
            evaluation = evaluationPortOut.save(evaluation);
        } else if (evaluation.getStatus().equals(EvaluationStatus.NONE) && !content.isBlank()) {
            evaluation.changeCreatedStatus();
            evaluation = evaluationPortOut.save(evaluation);
        }

        // Component 정보 조회
        List<Component> components = componentPortOut.getAllComponents(fullName);

        // Branch 목록 조회
        List<String> branches = repositoryPortOut.getAllBranches(repoInfo);

        // Response 생성
        return RepositoryPortInMapper.INSTANCE.toResponseRepository(
                repository,
                RepositoryPortInMapper.INSTANCE.toResponseEvaluation(evaluation),
                components.stream()
                        .map(RepositoryPortInMapper.INSTANCE::toResponseComponent)
                        .toList(),
                branches.toArray(String[]::new),
                content
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

    @Override
    public ResponseEvaluation evaluateReadme(RequestEvaluation request, Long userId, String owner, String name) {
        User user = userPortOut.getById(userId);
        String accessToken = cipherPortOut.decrypt(user.getGithubUser().getGithubToken());

        // readme 내용 조회
        String readmeContent = repositoryPortOut.getReadmeContent(
                new RepoBranchCommand(
                        new RepoInfoCommand(
                                accessToken,
                                owner,
                                name
                        ),
                        request.branch()
                )
        );

        ReadmeContext readmeContext = generateReadmeContext(
                owner,
                name,
                accessToken,
                request.branch()
        );

        ResponseEvaluation responseEvaluation = evaluate(
                new EvaluationCommand(
                        owner + "/" + name,
                        readmeContent,
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

        EvaluationStatus status = EvaluationStatus.getStatus(responseEvaluation.rating());

        Evaluation evaluation = evaluationPortOut.getByFullName(owner + "/" + name)
                .orElseGet(() -> Evaluation.createWithStatusEvaluation(
                        user.getId(),
                        owner + "/" + name,
                        responseEvaluation.rating(),
                        String.join("\n", responseEvaluation.contents())
                ));

        // 기존 Evaluation 업데이트
        evaluation.changeEvaluation(
                responseEvaluation.rating(),
                String.join("\n", responseEvaluation.contents()),
                status
        );
        evaluationPortOut.save(evaluation);

        return responseEvaluation;
    }

    @Override
    public ResponseEvaluation evaluateDraftReadme(RequestDraftEvaluation request, Long userId, String owner, String name) {
        User user = userPortOut.getById(userId);
        String accessToken = cipherPortOut.decrypt(user.getGithubUser().getGithubToken());

        ReadmeContext readmeContext = generateReadmeContext(
                owner,
                name,
                accessToken,
                request.branch()
        );

        return evaluate(
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
    }

    @Override
    public ResponseDraftReadme generateDraftReadme(RequestEvaluation request, Long userId, String owner, String name) {
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

        return new ResponseDraftReadme(draftReadme);
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

        // 커밋 목록 (최신, 중간, 초기 각 30개 이내)
        Optional<CommitResult> commitsOpt = repositoryPortOut.getCommitsByBranch(branchCommand);

        RepositoryInfoCommand.CommitCommand commitCommand = commitsOpt
                .map(commitResult -> new RepositoryInfoCommand.CommitCommand(
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
                .orElseGet(() -> new RepositoryInfoCommand.CommitCommand(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList()
                ));

        String latestShaKey = null;

        // 가장 최근 Commit SHA 조회
        if (commitsOpt.isPresent()) {
            CommitResult commitResult = commitsOpt.get();
            if (!commitResult.latestCommits().isEmpty()) {
                latestShaKey = commitResult.latestCommits().get(0).sha();
            }
        }

        GPTRepositoryInfo repositoryInfo;
        List<RepositoryLanguageResult> languages;
        List<RepositoryTreeResult> trees;
        List<RepositoryFileContentResult> entryContents;
        List<RepositoryFileContentResult> importantFileContents;

        if (latestShaKey != null) {
            LocalDateTime expiration = new CustomTimeStamp().getTimestamp().plusDays(3);

            languages = getLanguagesWithCache(repoInfoCommand, latestShaKey, expiration);
            trees = getTreesWithCache(branchCommand, latestShaKey, expiration);
            repositoryInfo = getTechStackWithCache(
                    owner, name, latestShaKey,
                    new RepositoryInfoCommand(
                            languages,
                            commitCommand,
                            trees
                    ),
                    expiration
            );
            entryContents = getEntryContentsWithCache(branchCommand, repositoryInfo, latestShaKey, expiration);
            importantFileContents = getImportantFileContentsWithCache(
                    branchCommand,
                    new RepositoryImportantCommand(
                            owner + "/" + name,
                            new RepositoryInfoCommand(
                                    languages,
                                    commitCommand,
                                    trees
                            ),
                            entryContents,
                            repositoryInfo.techStack(),
                            repositoryInfo.projectSize()
                    ),
                    latestShaKey, expiration
            );
        } else {
            languages = repositoryPortOut.getRepositoryLanguages(repoInfoCommand);
            trees = repositoryPortOut.getRepositoryTree(branchCommand);

            repositoryInfo = gptPortOut.getRepositoryInfo(
                    owner + "/" + name,
                    new RepositoryInfoCommand(
                            languages,
                            commitCommand,
                            trees
                    )
            );

            entryContents = fetchFileContents(
                    branchCommand,
                    getFilePaths(repositoryInfo)
            );

            List<RepositoryTreeResult> importantFiles = gptPortOut.getImportantFiles(
                    new RepositoryImportantCommand(
                            owner + "/" + name,
                            new RepositoryInfoCommand(
                                    languages,
                                    commitCommand,
                                    trees
                            ),
                            entryContents,
                            repositoryInfo.techStack(),
                            repositoryInfo.projectSize()
                    )
            );

            importantFileContents = fetchFileContents(
                    branchCommand,
                    importantFiles.stream()
                            .map(RepositoryTreeResult::path)
                            .toList()
            );
        }

        return new ReadmeContext(
                commitCommand,
                repositoryInfo,
                languages,
                trees,
                entryContents,
                importantFileContents
        );
    }

    private ResponseEvaluation evaluate(EvaluationCommand command) {
        EvaluationContent evaluationResponse = gptPortOut.evaluateReadme(command);
        return new ResponseEvaluation(
                evaluationResponse.rating(),
                evaluationResponse.contents()
        );
    }

    private <T> T getOrLoadAndCache(
            String key,
            CacheLoader<T> loader,
            TypeReference<T> typeReference,
            LocalDateTime expiration
    ) {
        T cachedData = redisPortOut.getObject(key, typeReference);

        if (cachedData != null) {
            return cachedData;
        }

        T data = loader.load();
        if (data != null) {
            redisPortOut.setObject(
                    key,
                    data,
                    expiration
            );
        }

        return data;
    }

    private List<RepositoryLanguageResult> getLanguagesWithCache(
            RepoInfoCommand command,
            String sha,
            LocalDateTime expiration
    ) {
        String key = RedisKeyFactory.createLanguageKey(
                command.owner(),
                command.name(),
                sha
        );

        return getOrLoadAndCache(
                key,
                () -> repositoryPortOut.getRepositoryLanguages(command),
                new TypeReference<List<RepositoryLanguageResult>>() {},
                expiration
        );
    }

    private List<RepositoryTreeResult> getTreesWithCache(
            RepoBranchCommand command,
            String sha,
            LocalDateTime expiration
    ) {
        String key = RedisKeyFactory.createTreeKey(
                command.repoInfo().owner(),
                command.repoInfo().name(),
                sha
        );

        return getOrLoadAndCache(
                key,
                () -> repositoryPortOut.getRepositoryTree(command),
                new TypeReference<List<RepositoryTreeResult>>() {},
                expiration
        );
    }

    private GPTRepositoryInfo getTechStackWithCache(
            String owner,
            String name,
            String sha,
            RepositoryInfoCommand repositoryInfo,
            LocalDateTime expiration
    ) {
        String key = RedisKeyFactory.createTechStackKey(owner, name, sha);

        return getOrLoadAndCache(
                key,
                () -> gptPortOut.getRepositoryInfo(
                        owner + "/" + name,
                        repositoryInfo
                ),
                new TypeReference<GPTRepositoryInfo>() {},
                expiration
        );
    }

    private List<RepositoryFileContentResult> getEntryContentsWithCache(
            RepoBranchCommand command,
            GPTRepositoryInfo repositoryInfo,
            String sha,
            LocalDateTime expiration
    ) {
        String key = RedisKeyFactory.createFileV1Key(
                command.repoInfo().owner(),
                command.repoInfo().name(),
                sha
        );

        return getOrLoadAndCache(
                key,
                () -> fetchFileContents(
                        command,
                        getFilePaths(repositoryInfo)
                ),
                new TypeReference<List<RepositoryFileContentResult>>() {
                },
                expiration
        );
    }

    private List<RepositoryFileContentResult> getImportantFileContentsWithCache(
            RepoBranchCommand command,
            RepositoryImportantCommand importantCommand,
            String sha,
            LocalDateTime expiration
    ) {
        String key = RedisKeyFactory.createFileV2Key(
                command.repoInfo().owner(),
                command.repoInfo().name(),
                sha
        );

        return getOrLoadAndCache(
                key,
                () -> {
                    List<RepositoryTreeResult> importantFiles = gptPortOut.getImportantFiles(importantCommand);

                    return fetchFileContents(
                            command,
                            importantFiles.stream()
                                    .map(RepositoryTreeResult::path)
                                    .toList()
                    );
                },
                new TypeReference<List<RepositoryFileContentResult>>() {},
                expiration
        );
    }

    private List<String> getFilePaths(
            GPTRepositoryInfo repositoryInfo
    ) {
        return Arrays.stream(repositoryInfo.entryPoints())
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

            if (contentResult.content().isBlank()) {
                continue;
            }

            fileContents.add(contentResult);
        }

        return fileContents;
    }
}
