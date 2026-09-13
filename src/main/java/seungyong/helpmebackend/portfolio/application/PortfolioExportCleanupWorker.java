package seungyong.helpmebackend.portfolio.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioExportPortOut;
import seungyong.helpmebackend.portfolio.application.port.out.PortfolioStoragePortOut;
import seungyong.helpmebackend.portfolio.domain.entity.PortfolioExport;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioExportCleanupWorker {
    private final PortfolioExportPortOut exportPortOut;
    private final PortfolioStoragePortOut storagePortOut;

    @Scheduled(cron = "${workers.portfolio-export-cleanup.cron:0 20 * * * *}")
    public void cleanup() {
        for (PortfolioExport export : exportPortOut.findExpiredPdf(OffsetDateTime.now(ZoneOffset.UTC), 100)) {
            try {
                // Storage 삭제 성공 후 DB 경로를 제거하여 실패 시 다음 배치가 다시 정리할 수 있도록 처리
                storagePortOut.delete(export.storagePath());
                exportPortOut.markExpired(export.id());
            } catch (RuntimeException exception) {
                log.warn("Portfolio PDF cleanup failed: exportId={}", export.id());
            }
        }
    }
}
