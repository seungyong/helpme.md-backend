package seungyong.helpmebackend.usecase.port.in.section;

import seungyong.helpmebackend.adapter.in.web.dto.section.request.RequestReorder;
import seungyong.helpmebackend.adapter.in.web.dto.section.request.RequestSectionContent;
import seungyong.helpmebackend.adapter.in.web.dto.section.response.ResponseSections;

public interface SectionPortIn {
    ResponseSections getSections(Long userId, String owner, String name);
    ResponseSections.Section createSection(Long userId, String owner, String name, String title);
    ResponseSections initSections(Long userId, String owner, String name, String branch, String splitMode);
    void updateSectionContent(Long userId, String owner, String name, RequestSectionContent request);
    void reorderSections(Long userId, String owner, String name, RequestReorder request);
    void deleteSection(Long userId, String owner, String name, Long sectionId);
}
