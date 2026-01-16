package seungyong.helpmebackend.adapter.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import seungyong.helpmebackend.adapter.out.persistence.entity.ComponentJpaEntity;
import seungyong.helpmebackend.adapter.out.persistence.mapper.ComponentPortOutMapper;
import seungyong.helpmebackend.adapter.out.persistence.repository.ComponentJpaRepository;
import seungyong.helpmebackend.domain.entity.component.Component;
import seungyong.helpmebackend.usecase.port.out.component.ComponentPortOut;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ComponentAdapter implements ComponentPortOut {
    private final ComponentJpaRepository componentJpaRepository;

    @Override
    public List<Component> getAllComponents(String repoFullName) {
        List<ComponentJpaEntity> entities = componentJpaRepository.findByRepoFullName(repoFullName);
        return entities.stream().map(ComponentPortOutMapper.INSTANCE::toDomain).toList();
    }
}
