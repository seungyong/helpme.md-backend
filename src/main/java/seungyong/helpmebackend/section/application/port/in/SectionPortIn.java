package seungyong.helpmebackend.section.application.port.in;

import seungyong.helpmebackend.section.adapter.in.web.dto.request.RequestReorder;
import seungyong.helpmebackend.section.adapter.in.web.dto.request.RequestSection;
import seungyong.helpmebackend.section.adapter.in.web.dto.request.RequestSectionContent;
import seungyong.helpmebackend.section.adapter.in.web.dto.response.ResponseSections;

public interface SectionPortIn {
    ResponseSections getSections(Long userId, String owner, String name);
    ResponseSections.Section createSection(Long userId, String owner, String name, RequestSection request);
    ResponseSections initSections(Long userId, String owner, String name, String branch, String splitMode);
    void updateSectionContent(Long userId, String owner, String name, RequestSectionContent request);
    void reorderSections(Long userId, String owner, String name, RequestReorder request);
    void deleteSection(Long userId, String owner, String name, Long sectionId);
}
