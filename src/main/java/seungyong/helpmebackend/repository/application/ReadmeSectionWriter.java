package seungyong.helpmebackend.repository.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.repository.application.port.in.result.GeneratedReadmeResult;
import seungyong.helpmebackend.section.application.port.out.SectionPortOut;
import seungyong.helpmebackend.section.domain.entity.Section;

import java.util.List;

@Service
@RequiredArgsConstructor
class ReadmeSectionWriter {
    private final ProjectPortOut projectPortOut;
    private final SectionPortOut sectionPortOut;

    @Transactional
    public GeneratedReadmeResult replace(
            Long userId,
            String repositoryFullName,
            String generatedReadme
    ) {
        Project project = projectPortOut.getByUserIdAndRepoFullName(userId, repositoryFullName)
                .orElseGet(() -> projectPortOut.save(
                        new Project(null, userId, repositoryFullName)
                ));

        if (!sectionPortOut.getSectionsByUserIdAndRepoFullName(userId, repositoryFullName).isEmpty()) {
            sectionPortOut.deleteAllByUserIdAndRepoFullName(userId, repositoryFullName);
        }

        List<Section> sections = sectionPortOut.saveAll(
                Section.splitContent(project.getId(), generatedReadme, Section.SplitMode.SPLIT)
        );

        return new GeneratedReadmeResult(
                sections.stream()
                        .map(section -> new GeneratedReadmeResult.Section(
                                section.getId(),
                                section.getTitle(),
                                section.getContent(),
                                section.getOrderIdx()
                        ))
                        .toList()
        );
    }
}
