package seungyong.helpmebackend.usecase.service.github;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.adapter.in.web.dto.repository.request.RequestDraftEvaluation;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.ResponseEvaluation;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.ResponseRepositories;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.ResponseRepository;
import seungyong.helpmebackend.adapter.in.web.mapper.RepositoryPortInMapper;
import seungyong.helpmebackend.adapter.out.result.RepositoryDetailResult;
import seungyong.helpmebackend.adapter.out.result.RepositoryFileContentResult;
import seungyong.helpmebackend.adapter.out.result.RepositoryResult;
import seungyong.helpmebackend.adapter.out.result.RepositoryTreeResult;
import seungyong.helpmebackend.domain.entity.component.Component;
import seungyong.helpmebackend.domain.entity.evaluation.Evaluation;
import seungyong.helpmebackend.domain.entity.user.User;
import seungyong.helpmebackend.domain.vo.EvaluationStatus;
import seungyong.helpmebackend.infrastructure.gpt.EvaluationContent;
import seungyong.helpmebackend.usecase.port.in.repository.RepositoryPortIn;
import seungyong.helpmebackend.usecase.port.out.cipher.CipherPortOut;
import seungyong.helpmebackend.usecase.port.out.component.ComponentPortOut;
import seungyong.helpmebackend.usecase.port.out.evaluation.EvaluationPortOut;
import seungyong.helpmebackend.usecase.port.out.github.repository.RepositoryPortOut;
import seungyong.helpmebackend.usecase.port.out.gpt.GPTPortOut;
import seungyong.helpmebackend.usecase.port.out.user.UserPortOut;

import java.util.ArrayList;
import java.util.List;

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

        // Repository 정보 조회
        RepositoryDetailResult repository = repositoryPortOut.getRepository(accessToken, owner, name);
        String fullName = owner + "/" + name;

        // ReadME.md 내용 조회
        String content = repositoryPortOut.getReadmeContent(accessToken, owner, name);

        // Evaluation 정보 조회 및 없으면 생성
        Evaluation evaluation = evaluationPortOut.getByFullName(fullName);

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
                    0.0f,
                    "",
                    EvaluationStatus.CREATED
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
        List<String> branches = repositoryPortOut.getAllBranches(accessToken, owner, name);

        // Response 생성
        return RepositoryPortInMapper.INSTANCE.toResponseRepository(
                repository,
                RepositoryPortInMapper.INSTANCE.toResponseEvaluation(evaluation),
                components.stream()
                        .map(RepositoryPortInMapper.INSTANCE::toResponseComponent)
                        .toList(),
                String.join(",", branches),
                content
        );
    }

    @Override
    public ResponseEvaluation evaluateDraftReadme(RequestDraftEvaluation request, Long userId, String owner, String name) {
        User user = userPortOut.getById(userId);
        String accessToken = cipherPortOut.decrypt(user.getGithubUser().getGithubToken());

        // 커밋 목록 조회 (최신 200개)
        List<String> commits = repositoryPortOut.getCommitsByBranch(accessToken, owner, name, request.branch());

        // 프로젝트 구조 조회
        List<RepositoryTreeResult> trees = repositoryPortOut.getRepositoryTree(accessToken, owner, name, request.branch());

        // 핵심 파일 선별 (구조 -> 파일명으로 판단)
        List<RepositoryTreeResult> importantFiles = gptPortOut.getImportantFiles(trees);

        // 코드 내용 조회
        List<RepositoryFileContentResult> fileContents = new ArrayList<>();
        for (RepositoryTreeResult file : importantFiles) {
            RepositoryFileContentResult contentResult = repositoryPortOut.getFileContent(
                    accessToken,
                    owner,
                    name,
                    file
            );

            if (contentResult == null || contentResult.content().isBlank()) {
                continue;
            }

            fileContents.add(contentResult);
        }

        // AI 평가 요청 및 결과 수신 (request 사용하여, 초안 평가 요청)
        EvaluationContent evaluationResponse = gptPortOut.evaluateReadme(
                request.content(),
                commits,
                trees,
                fileContents
        );

        return new ResponseEvaluation(
                evaluationResponse.rating(),
                evaluationResponse.contents()
        );
    }
}
