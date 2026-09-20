package seungyong.helpmebackend.project.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.portfolio.application.port.in.PortfolioDeletionPortIn;
import seungyong.helpmebackend.project.application.port.out.ProjectDeletionPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;

@Component
@RequiredArgsConstructor
public class ProjectDeletionProcessor {
    private final ProjectDeletionPortOut deletionPortOut;
    private final PortfolioDeletionPortIn portfolioDeletionPortIn;

    @Transactional
    public void cleanup(Project project) {
        // 파일 업로드와 hard delete 직렬화를 위해 외부 정리 완료까지 프로젝트 행 잠금 유지
        if (!deletionPortOut.lockClaim(project.getId(), project.getUpdatedAt())) return;
        portfolioDeletionPortIn.deleteProjectAssets(project.getId());
        deletionPortOut.hardDelete(project.getId());
    }
}
