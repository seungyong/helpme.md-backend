package seungyong.helpmebackend.adapter.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import seungyong.helpmebackend.adapter.out.persistence.entity.ComponentJpaEntity;
import seungyong.helpmebackend.adapter.out.persistence.mapper.ComponentPortOutMapper;
import seungyong.helpmebackend.adapter.out.persistence.repository.ComponentJpaRepository;
import seungyong.helpmebackend.domain.entity.component.Component;
import seungyong.helpmebackend.usecase.port.out.component.ComponentPortOut;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ComponentAdapter implements ComponentPortOut {
    private final ComponentJpaRepository componentJpaRepository;

    @Override
    public Component save(Component component) {
        ComponentJpaEntity savedEntity = componentJpaRepository.save(
                ComponentPortOutMapper.INSTANCE.toEntity(component)
        );
        return ComponentPortOutMapper.INSTANCE.toDomain(savedEntity);
    }

    @Override
    public void delete(Component component) {
        componentJpaRepository.delete(
                ComponentPortOutMapper.INSTANCE.toEntityWithId(component)
        );
    }

    @Override
    public List<Component> getAllComponents(String owner, String name, Long userId) {
        String fullName = Component.getFullName(owner, name);
        List<ComponentJpaEntity> entities = componentJpaRepository.findByRepoFullNameAndUser_IdOrderByUpdatedAtDesc(fullName, userId);
        return entities.stream().map(ComponentPortOutMapper.INSTANCE::toDomain).toList();
    }

    @Override
    public Optional<Component> getComponentById(Long componentId, Long userId) {
        return componentJpaRepository.findByIdAndUser_Id(componentId, userId)
                .map(ComponentPortOutMapper.INSTANCE::toDomain);
    }
}
