package seungyong.helpmebackend.usecase.port.out.component;

import seungyong.helpmebackend.domain.entity.component.Component;

import java.util.List;
import java.util.Optional;

public interface ComponentPortOut {
    Component save(Component component);
    void delete(Component component);

    List<Component> getAllComponents(String owner, String name, Long userId);
    Optional<Component> getComponentById(Long componentId, Long userId);
}

