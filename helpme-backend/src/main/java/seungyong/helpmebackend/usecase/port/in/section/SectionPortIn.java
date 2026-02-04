package seungyong.helpmebackend.usecase.port.in.section;

import seungyong.helpmebackend.adapter.in.web.dto.section.response.ResponseSections;

public interface SectionPortIn {
    ResponseSections getSections(Long userId, String owner, String name);
    ResponseSections.Section createSection(Long userId, String owner, String name, String title);
    ResponseSections initSections(Long userId, String owner, String name, String branch, String splitMode);
}
