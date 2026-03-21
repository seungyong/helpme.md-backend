package seungyong.helpmebackend.section.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.global.application.port.out.RedisPortOut;
import seungyong.helpmebackend.global.domain.type.RedisKey;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.repository.application.port.out.CipherPortOut;
import seungyong.helpmebackend.repository.application.port.out.RepositoryPortOut;
import seungyong.helpmebackend.repository.application.port.out.command.RepoBranchCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepoInfoCommand;
import seungyong.helpmebackend.repository.application.port.out.command.RepoPermissionCommand;
import seungyong.helpmebackend.repository.domain.exception.RepositoryErrorCode;
import seungyong.helpmebackend.section.adapter.in.web.dto.request.RequestReorder;
import seungyong.helpmebackend.section.adapter.in.web.dto.request.RequestSection;
import seungyong.helpmebackend.section.adapter.in.web.dto.request.RequestSectionContent;
import seungyong.helpmebackend.section.adapter.in.web.dto.response.ResponseSections;
import seungyong.helpmebackend.section.application.port.in.SectionPortIn;
import seungyong.helpmebackend.section.application.port.in.SectionPortInMapper;
import seungyong.helpmebackend.section.application.port.out.SectionPortOut;
import seungyong.helpmebackend.section.domain.entity.Section;
import seungyong.helpmebackend.section.domain.exception.SectionErrorCode;
import seungyong.helpmebackend.user.application.port.out.UserPortOut;
import seungyong.helpmebackend.user.domain.entity.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SectionService implements SectionPortIn {
    private final UserPortOut userPortOut;
    private final ProjectPortOut projectPortOut;
    private final SectionPortOut sectionPortOut;
    private final RepositoryPortOut repositoryPortOut;
    private final CipherPortOut cipherPortOut;
    private final RedisPortOut redisPortOut;

    @Override
    public ResponseSections getSections(Long userId, String owner, String name) {
        User user = userPortOut.getById(userId);
        checkAccessRepository(user, owner, name);
        String fullName = owner + "/" + name;

        // Section 목록 조회
        List<Section> sections = sectionPortOut.getSectionsByUserIdAndRepoFullName(userId, fullName);

        if (sections.isEmpty()) {
            throw new CustomException(SectionErrorCode.NOT_FOUND_SECTIONS);
        }

        return new ResponseSections(sections.stream()
                .map(SectionPortInMapper.INSTANCE::toResponseSection)
                .toList());
    }

    @Override
    public ResponseSections.Section createSection(Long userId, String owner, String name, RequestSection request) {
        User user = userPortOut.getById(userId);
        checkAccessRepository(user, owner, name);
        String fullName = owner + "/" + name;

        Project project = projectPortOut.getByUserIdAndRepoFullName(userId, fullName)
                .orElseGet(() -> projectPortOut.save(new Project(null, userId, fullName)));

        Integer lastOrderIdx = sectionPortOut.lastOrderIdxByUserIdAndRepoFullName(userId, fullName)
                .orElse(0);

        String content = request.content() == null || request.content().isBlank() ?
                "## " + request.title() + "\n\n" : request.content();
        Section section = new Section(
                null,
                project.getId(),
                request.title(),
                content,
                lastOrderIdx + 1
        );

        Section savedSection = sectionPortOut.save(section);
        return SectionPortInMapper.INSTANCE.toResponseSection(savedSection);
    }

    @Transactional
    @Override
    public ResponseSections initSections(Long userId, String owner, String name, String branch, String splitMode) {
        User user = userPortOut.getById(userId);
        String accessToken = cipherPortOut.decrypt(user.getGithubUser().getGithubToken().value());
        checkAccessRepository(user, owner, name);
        String fullName = owner + "/" + name;

        if (!(splitMode.equals("split") || splitMode.equals("whole"))) {
            splitMode = "whole";
        }

        Project project = projectPortOut.getByUserIdAndRepoFullName(userId, fullName)
                .orElseGet(() -> projectPortOut.save(new Project(null, userId, fullName)));

        List<Section> existingSections = sectionPortOut.getSectionsByUserIdAndRepoFullName(userId, fullName);

        if (!existingSections.isEmpty()) {
            sectionPortOut.deleteAllByUserIdAndRepoFullName(userId, fullName);
        }

        String readmeContent = repositoryPortOut.getReadmeContent(new RepoBranchCommand(
                new RepoInfoCommand(
                        accessToken,
                        owner,
                        name
                ),
                branch
        ));

        if (readmeContent.isEmpty()) {
            Section savedSection = sectionPortOut.save(new Section(
                    null,
                    project.getId(),
                    "Untitled Section",
                    "",
                    1
            ));
            return new ResponseSections(List.of(SectionPortInMapper.INSTANCE.toResponseSection(savedSection)));
        }

        List<Section> savedSections = sectionPortOut.saveAll(
                Section.splitContent(
                        project.getId(),
                        readmeContent,
                        splitMode.equals("split") ? Section.SplitMode.SPLIT : Section.SplitMode.WHOLE
                )
        );
        return new ResponseSections(savedSections.stream()
                .map(SectionPortInMapper.INSTANCE::toResponseSection)
                .toList());
    }

    @Override
    public void updateSectionContent(Long userId, String owner, String name, Long sectionId, RequestSectionContent request) {
        User user = userPortOut.getById(userId);
        checkAccessRepository(user, owner, name);

        Section section = sectionPortOut.getByIdAndUserId(sectionId, userId)
                .orElseThrow(() -> new CustomException(SectionErrorCode.NOT_FOUND_SECTIONS));

        section.updateContent(request.content());
        sectionPortOut.save(section);
    }

    @Override
    public void reorderSections(Long userId, String owner, String name, RequestReorder request) {
        User user = userPortOut.getById(userId);
        checkAccessRepository(user, owner, name);

        List<Section> sections = sectionPortOut.getSectionsByUserIdAndRepoFullName(userId, owner + "/" + name);

        if (sections.size() != request.sectionIds().size()) {
            throw new CustomException(SectionErrorCode.INVALID_REORDER_REQUEST);
        }

        for (int i = 0; i < request.sectionIds().size(); i++) {
            Long sectionId = request.sectionIds().get(i);
            Section section = sections.stream()
                    .filter(s -> s.getId().equals(sectionId))
                    .findFirst()
                    .orElseThrow(() -> new CustomException(SectionErrorCode.NOT_FOUND_SECTIONS));

            section.updateOrderIdx(i + 1);
        }

        sectionPortOut.saveAll(sections);
    }

    @Transactional
    @Override
    public void deleteSection(Long userId, String owner, String name, Long sectionId) {
        User user = userPortOut.getById(userId);
        checkAccessRepository(user, owner, name);

        Section section = sectionPortOut.getByIdAndUserId(sectionId, userId)
                .orElseThrow(() -> new CustomException(SectionErrorCode.NOT_FOUND_SECTIONS));

        // 섹션 재정렬
        sectionPortOut.decreaseOrderIdxAfter(
                userId,
                owner + "/" + name,
                section.getOrderIdx()
        );

        sectionPortOut.delete(section);
    }

    private void checkAccessRepository(User user, String owner, String name) {
        String redisKey = RedisKey.GITHUB_AUTH_KEY.getValue() + user.getId() + ":" + owner + "/" + name;

        if (redisPortOut.exists(redisKey)) {
            return;
        }

        RepoPermissionCommand command = new RepoPermissionCommand(
                new RepoInfoCommand(
                        cipherPortOut.decrypt(user.getGithubUser().getGithubToken().value()),
                        owner,
                        name
                ),
                user.getGithubUser().getName()
        );

        if (!repositoryPortOut.checkPermission(command)) {
            throw new CustomException(RepositoryErrorCode.REPOSITORY_FORBIDDEN);
        }

        redisPortOut.set(redisKey, "authorized", Instant.now().plus(10, ChronoUnit.MINUTES));
    }
}
