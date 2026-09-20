package seungyong.helpmebackend.portfolio.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import seungyong.helpmebackend.portfolio.application.port.in.PortfolioDeletionPortIn;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioExportPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioStoragePortOut;

@Service
@RequiredArgsConstructor
public class PortfolioDeletionService implements PortfolioDeletionPortIn {
    private final PortfolioExportPortOut exportPortOut;
    private final PortfolioStoragePortOut storagePortOut;

    @Override
    public void deleteProjectAssets(Long projectId) {
        // 호출자가 잡은 프로젝트 잠금 안에서 모든 확정 경로를 정리한 후 hard delete 허용
        for (String storagePath : exportPortOut.findPdfStoragePathsByProjectId(projectId)) {
            storagePortOut.delete(storagePath);
        }
    }
}
