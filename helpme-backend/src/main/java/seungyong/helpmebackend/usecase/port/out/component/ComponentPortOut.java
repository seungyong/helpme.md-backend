package seungyong.helpmebackend.usecase.port.out.component;

import seungyong.helpmebackend.domain.entity.component.Component;

import java.util.List;

public interface ComponentPortOut {
    List<Component> getAllComponents(String repoFullName);
}

