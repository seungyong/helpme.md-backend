package seungyong.helpmebackend.notion.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import seungyong.helpmebackend.notion.adapter.out.persistence.entity.NotionConnectionJpaEntity;

import java.util.Optional;

interface NotionConnectionJpaRepository
        extends JpaRepository<NotionConnectionJpaEntity, Long> {
    Optional<NotionConnectionJpaEntity> findByUser_Id(Long userId);
}
