package seungyong.helpmebackend.usecase.service.section;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import seungyong.helpmebackend.usecase.port.in.section.SectionPortIn;
import seungyong.helpmebackend.usecase.port.out.cipher.CipherPortOut;
import seungyong.helpmebackend.usecase.port.out.github.repository.RepositoryPortOut;
import seungyong.helpmebackend.usecase.port.out.project.ProjectPortOut;
import seungyong.helpmebackend.usecase.port.out.section.SectionPortOut;
import seungyong.helpmebackend.usecase.port.out.user.UserPortOut;

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

    @Transactional
    @Override
    public ResponseSections getSections(Long userId, String owner, String name) {
        User user = userPortOut.getById(userId);

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
    public ResponseSections.Section createSection(Long userId, String owner, String name, String title) {
        User user = userPortOut.getById(userId);

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

        String fullName = owner + "/" + name;

        Project project = projectPortOut.getByUserIdAndRepoFullName(userId, fullName)
                .orElseGet(() -> projectPortOut.save(new Project(null, userId, fullName)));

        Short lastOrderIdx = sectionPortOut.lastOrderIdxByUserIdAndRepoFullName(userId, fullName);

        Section section = new Section(
                null,
                project.getId(),
                title,
                "## " + title + "\n",
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

        List<Section> sections = new ArrayList<>();
        String[] splitContents = splitReadmeContent(readmeContent, splitMode);

        for (int i = 0; i < splitContents.length; i++) {
            String content = splitContents[i];
            String title = content.lines()
                    .findFirst()
                    .orElse("Untitled Section")
                    .replaceAll("^(#{1,2} )", "");

            Section section = new Section(
                    null,
                    project.getId(),
                    title,
                    content.trim(),
                    (short) (i + 1)
            );

            sections.add(section);
        }

        List<Section> savedSections = sectionPortOut.saveAll(sections);
        return new ResponseSections(savedSections.stream()
                .map(SectionPortInMapper.INSTANCE::toResponseSection)
                .toList());
    }

    private String[] splitReadmeContent(String content, String splitMode) {
        if (splitMode.equals("split")) {
            return content.split("(?m)(?=^#{1,2} )");
        } else {
            return new String[] { content };
        }
    }
}
