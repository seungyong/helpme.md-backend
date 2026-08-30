package seungyong.helpmebackend.section.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.section.application.port.in.ReadmeComponentPortIn;
import seungyong.helpmebackend.section.application.port.in.command.CreateReadmeComponentCommand;
import seungyong.helpmebackend.section.application.port.in.command.DeleteReadmeComponentCommand;
import seungyong.helpmebackend.section.application.port.in.command.UpdateReadmeComponentCommand;
import seungyong.helpmebackend.section.application.port.out.SectionPortOut;
import seungyong.helpmebackend.section.domain.entity.Section;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReadmeComponentService implements ReadmeComponentPortIn {
    private final ReadmeComponentRepositoryAccessResolver repositoryAccessResolver;
    private final ReadmeComponentWriter componentWriter;
    private final SectionPortOut sectionPortOut;

    @Override
    public List<Section> getComponents(Long userId, String owner, String name) {
        Project project = repositoryAccessResolver.resolveWritable(userId, owner, name);
        return sectionPortOut.getSectionsByUserIdAndRepoFullName(
                project.getUserId(), project.getRepoFullName()
        );
    }

    @Override
    public Section createComponent(CreateReadmeComponentCommand command) {
        validateCreateCommand(command);
        Project project = repositoryAccessResolver.resolveWritable(
                command.userId(), command.owner(), command.name()
        );
        return componentWriter.create(
                project,
                command.title().trim(),
                command.content() == null ? "" : command.content(),
                command.orderIdx()
        );
    }

    @Override
    public Section updateComponent(UpdateReadmeComponentCommand command) {
        validateUpdateCommand(command);
        Project project = repositoryAccessResolver.resolveWritable(
                command.userId(), command.owner(), command.name()
        );
        return componentWriter.update(
                project,
                command.componentId(),
                command.title() == null ? null : command.title().trim(),
                command.content(),
                command.orderIdx(),
                command.version()
        );
    }

    @Override
    public void deleteComponent(DeleteReadmeComponentCommand command) {
        validateDeleteCommand(command);
        Project project = repositoryAccessResolver.resolveWritable(
                command.userId(), command.owner(), command.name()
        );
        componentWriter.delete(project, command.componentId(), command.version());
    }

    private void validateCreateCommand(CreateReadmeComponentCommand command) {
        if (command == null
                || command.userId() == null
                || !StringUtils.hasText(command.owner())
                || !StringUtils.hasText(command.name())
                || !StringUtils.hasText(command.title())
                || command.orderIdx() != null && command.orderIdx() < 0) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private void validateUpdateCommand(UpdateReadmeComponentCommand command) {
        if (command == null
                || command.userId() == null
                || !StringUtils.hasText(command.owner())
                || !StringUtils.hasText(command.name())
                || command.componentId() == null
                || command.componentId() < 1
                || command.version() == null
                || command.version() < 0
                || command.orderIdx() != null && command.orderIdx() < 0
                || command.title() != null && !StringUtils.hasText(command.title())) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private void validateDeleteCommand(DeleteReadmeComponentCommand command) {
        if (command == null
                || command.userId() == null
                || !StringUtils.hasText(command.owner())
                || !StringUtils.hasText(command.name())
                || command.componentId() == null
                || command.componentId() < 1
                || command.version() == null
                || command.version() < 0) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }
}
