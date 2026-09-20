package seungyong.helpmebackend.portfolio.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioExportPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioStoragePortOut;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class PortfolioDeletionServiceTest {
    @Mock private PortfolioExportPortOut exportPortOut;
    @Mock private PortfolioStoragePortOut storagePortOut;

    @Test
    @DisplayName("프로젝트의 모든 PDF Storage 경로를 멱등 삭제")
    void deletesAllProjectPdfAssets() {
        given(exportPortOut.findPdfStoragePathsByProjectId(101L)).willReturn(List.of(
                "portfolios/501/601.pdf",
                "portfolios/502/602.pdf"
        ));

        new PortfolioDeletionService(exportPortOut, storagePortOut)
                .deleteProjectAssets(101L);

        InOrder order = inOrder(storagePortOut);
        order.verify(storagePortOut).delete("portfolios/501/601.pdf");
        order.verify(storagePortOut).delete("portfolios/502/602.pdf");
    }
}
