package seungyong.helpmebackend.usecase.service.component;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import seungyong.helpmebackend.adapter.in.web.dto.component.request.RequestComponent;
import seungyong.helpmebackend.adapter.in.web.dto.component.response.ResponseComponents;
import seungyong.helpmebackend.adapter.in.web.dto.component.response.ResponseCreatedComponent;
import seungyong.helpmebackend.adapter.in.web.mapper.ComponentPortInMapper;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.domain.entity.component.Component;
import seungyong.helpmebackend.domain.exception.ComponentErrorCode;
import seungyong.helpmebackend.usecase.port.in.component.ComponentPortIn;
import seungyong.helpmebackend.usecase.port.out.component.ComponentPortOut;

@Service
@RequiredArgsConstructor
public class ComponentService implements ComponentPortIn {
    private final ComponentPortOut componentPortOut;

    @Override
    public ResponseComponents getComponents(String owner, String name, Long userId) {
        return new ResponseComponents(
                componentPortOut.getAllComponents(owner, name, userId)
                        .stream()
                        .map(ComponentPortInMapper.INSTANCE::toResponseComponent)
                        .toList()
        );
    }

    @Override
    public ResponseCreatedComponent createComponent(RequestComponent request,  String owner, String name, Long userId) {
        return new ResponseCreatedComponent(
                ComponentPortInMapper.INSTANCE.toResponseComponent(
                        componentPortOut.save(
                                ComponentPortInMapper.INSTANCE.toDomain(request, Component.getFullName(owner, name), userId)
                        )
                ).id()
        );
    }

    @Override
    public void updateComponent(RequestComponent request, Long componentId, Long userId) {
        Component component = componentPortOut.getComponentById(componentId, userId)
                .orElseThrow(() -> new CustomException(ComponentErrorCode.COMPONENT_NOT_FOUND));

        component.setTitle(request.title());
        component.setContent(request.content());

        componentPortOut.save(component);
    }

    @Override
    public void deleteComponent(Long componentId, Long userId) {
        Component component = componentPortOut.getComponentById(componentId, userId)
                .orElseThrow(() -> new CustomException(ComponentErrorCode.COMPONENT_NOT_FOUND));

        componentPortOut.delete(component);
    }
}
