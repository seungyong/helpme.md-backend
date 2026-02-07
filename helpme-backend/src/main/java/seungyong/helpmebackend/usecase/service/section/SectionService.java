package seungyong.helpmebackend.usecase.service.section;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seungyong.helpmebackend.adapter.in.web.dto.section.request.RequestReorder;
import seungyong.helpmebackend.adapter.in.web.dto.section.request.RequestSection;
import seungyong.helpmebackend.adapter.in.web.dto.section.request.RequestSectionContent;
import seungyong.helpmebackend.adapter.in.web.dto.section.response.ResponseSections;
import seungyong.helpmebackend.adapter.in.web.mapper.SectionPortInMapper;
import seungyong.helpmebackend.adapter.out.command.RepoBranchCommand;
import seungyong.helpmebackend.adapter.out.command.RepoInfoCommand;
import seungyong.helpmebackend.adapter.out.command.RepoPermissionCommand;
import seungyong.helpmebackend.common.exception.CustomException;
import seungyong.helpmebackend.domain.entity.project.Project;
import seungyong.helpmebackend.domain.entity.section.Section;
import seungyong.helpmebackend.domain.entity.user.User;
import seungyong.helpmebackend.domain.exception.RepositoryErrorCode;
import seungyong.helpmebackend.domain.exception.SectionErrorCode;
import seungyong.helpmebackend.infrastructure.redis.RedisKey;
import seungyong.helpmebackend.usecase.port.in.section.SectionPortIn;
import seungyong.helpmebackend.usecase.port.out.cipher.CipherPortOut;
import seungyong.helpmebackend.usecase.port.out.github.repository.RepositoryPortOut;
import seungyong.helpmebackend.usecase.port.out.project.ProjectPortOut;
import seungyong.helpmebackend.usecase.port.out.redis.RedisPortOut;
import seungyong.helpmebackend.usecase.port.out.section.SectionPortOut;
import seungyong.helpmebackend.usecase.port.out.user.UserPortOut;

import java.time.Instant;
import java.time.chrono.ChronoPeriod;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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

    @Transactional
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

        Short lastOrderIdx = sectionPortOut.lastOrderIdxByUserIdAndRepoFullName(userId, fullName);

        String content = request.content() == null || request.content().isBlank() ?
                "## " + request.title() + "\n\n" : request.content();
        Section section = new Section(
                null,
                project.getId(),
                request.title(),
                content,
                (short) (lastOrderIdx + 1)
        );

        Section savedSection = sectionPortOut.save(section);
        return SectionPortInMapper.INSTANCE.toResponseSection(savedSection);
    }

    @Transactional
    @Override
    public ResponseSections initSections(Long userId, String owner, String name, String branch, String splitMode) {
        User user = userPortOut.getById(userId);
        String accessToken = cipherPortOut.decrypt(user.getGithubUser().getGithubToken());
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
                    (short) 1
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
    public void updateSectionContent(Long userId, String owner, String name, RequestSectionContent request) {
        User user = userPortOut.getById(userId);
        checkAccessRepository(user, owner, name);

        Section section = sectionPortOut.getByIdAndUserId(request.id(), userId)
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

            section.updateOrderIdx((short) (i + 1));
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
                        cipherPortOut.decrypt(user.getGithubUser().getGithubToken()),
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
