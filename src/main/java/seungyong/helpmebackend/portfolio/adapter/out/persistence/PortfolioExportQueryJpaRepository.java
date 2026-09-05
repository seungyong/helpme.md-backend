package seungyong.helpmebackend.portfolio.adapter.out.persistence;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import seungyong.helpmebackend.portfolio.adapter.out.persistence.entity.PortfolioExportJpaEntity;

import java.util.List;

interface PortfolioExportQueryJpaRepository extends Repository<PortfolioExportJpaEntity, Long> {
    @Query("""
            select e from PortfolioExport e
            where e.portfolio.id in :portfolioIds
              and e.id = (select max(e2.id) from PortfolioExport e2 where e2.portfolio.id = e.portfolio.id)
            """)
    List<PortfolioExportJpaEntity> findLatestByPortfolioIds(
            @Param("portfolioIds") List<Long> portfolioIds
    );
}
