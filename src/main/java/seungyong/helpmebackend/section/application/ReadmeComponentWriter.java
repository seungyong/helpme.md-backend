package seungyong.helpmebackend.section.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.github.domain.exception.GithubErrorCode;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.DocumentErrorCode;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.section.application.port.out.SectionPortOut;
import seungyong.helpmebackend.section.domain.entity.Section;
import seungyong.helpmebackend.section.domain.exception.SectionErrorCode;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class ReadmeComponentWriter {
    private final SectionPortOut sectionPortOut;

    @Transactional
    public Section create(
            Project project,
            String title,
            String content,
            Integer requestedOrderIdx
    ) {
        lockProject(project.getId());

        Integer lastOrderIdx = sectionPortOut
                .lastOrderIdxByUserIdAndRepoFullName(
                        project.getUserId(), project.getRepoFullName()
                )
                .orElse(null);
        int targetOrderIdx = requestedOrderIdx == null
                ? nextOrderIdx(lastOrderIdx)
                : requestedOrderIdx;
        validateCreateOrderIdx(targetOrderIdx, lastOrderIdx);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        sectionPortOut.increaseOrderIdxFrom(
                project.getUserId(), project.getRepoFullName(), targetOrderIdx, now
        );
        return sectionPortOut.save(new Section(
                null,
                project.getId(),
                title,
                content,
                targetOrderIdx,
                0,
                null,
                null
        ));
    }

    @Transactional
    public Section update(
            Project project,
            Long componentId,
            String changedTitle,
            String changedContent,
            Integer changedOrderIdx,
            Integer expectedVersion
    ) {
        lockProject(project.getId());

        Section current = getCurrent(project, componentId);
        requireCurrentVersion(current, expectedVersion);

        int targetOrderIdx = changedOrderIdx == null
                ? current.getOrderIdx()
                : changedOrderIdx;
        int lastOrderIdx = sectionPortOut
                .lastOrderIdxByUserIdAndRepoFullName(
                        project.getUserId(), project.getRepoFullName()
                )
                .orElse(current.getOrderIdx());
        if (targetOrderIdx < 0 || targetOrderIdx > lastOrderIdx) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }

        String title = changedTitle == null ? current.getTitle() : changedTitle;
        String content = changedContent == null ? current.getContent() : changedContent;
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        sectionPortOut.shiftOrderIdxForMove(
                project.getUserId(),
                project.getRepoFullName(),
                componentId,
                current.getOrderIdx(),
                targetOrderIdx,
                now
        );
        return sectionPortOut.updateIfVersionMatches(
                        componentId,
                        project.getUserId(),
                        project.getRepoFullName(),
                        title,
                        content,
                        targetOrderIdx,
                        expectedVersion,
                        now
                )
                .orElseThrow(ReadmeComponentWriter::versionConflict);
    }

    @Transactional
    public void delete(Project project, Long componentId, Integer expectedVersion) {
        lockProject(project.getId());

        Section current = getCurrent(project, componentId);
        requireCurrentVersion(current, expectedVersion);

        if (!sectionPortOut.deleteIfVersionMatches(
                componentId,
                project.getUserId(),
                project.getRepoFullName(),
                expectedVersion
        )) {
            throw versionConflict();
        }
        sectionPortOut.decreaseOrderIdxAfterVersioned(
                project.getUserId(),
                project.getRepoFullName(),
                current.getOrderIdx(),
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }

    private Section getCurrent(Project project, Long componentId) {
        return sectionPortOut.getByIdAndUserIdAndRepoFullName(
                        componentId,
                        project.getUserId(),
                        project.getRepoFullName()
                )
                .orElseThrow(() -> new CustomException(
                        SectionErrorCode.NOT_FOUND_SECTIONS
                ));
    }

    private void lockProject(Long projectId) {
        if (!sectionPortOut.lockProject(projectId)) {
            throw new CustomException(GithubErrorCode.GITHUB_RESOURCE_NOT_FOUND);
        }
    }

    private int nextOrderIdx(Integer lastOrderIdx) {
        return lastOrderIdx == null ? 0 : lastOrderIdx + 1;
    }

    private void validateCreateOrderIdx(int targetOrderIdx, Integer lastOrderIdx) {
        if (targetOrderIdx < 0 || targetOrderIdx > nextOrderIdx(lastOrderIdx)) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private void requireCurrentVersion(Section current, Integer expectedVersion) {
        if (!current.getVersion().equals(expectedVersion)) {
            throw versionConflict();
        }
    }

    private static CustomException versionConflict() {
        return new CustomException(DocumentErrorCode.DOCUMENT_VERSION_CONFLICT);
    }
}
