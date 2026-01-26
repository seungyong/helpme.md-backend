package seungyong.helpmebackend.usecase.port.in.component;

import seungyong.helpmebackend.adapter.in.web.dto.component.request.RequestComponent;
import seungyong.helpmebackend.adapter.in.web.dto.component.response.ResponseComponents;
import seungyong.helpmebackend.adapter.in.web.dto.component.response.ResponseCreatedComponent;

public interface ComponentPortIn {
    ResponseComponents getComponents(String owner, String name, Long userId);
    ResponseCreatedComponent createComponent(RequestComponent request, String owner, String name, Long userId);
    void updateComponent(RequestComponent request, Long componentId, Long userId);
    void deleteComponent(Long componentId, Long userId);
}
