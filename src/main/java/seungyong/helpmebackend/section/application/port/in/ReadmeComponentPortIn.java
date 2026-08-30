package seungyong.helpmebackend.section.application.port.in;

import seungyong.helpmebackend.section.application.port.in.command.CreateReadmeComponentCommand;
import seungyong.helpmebackend.section.application.port.in.command.DeleteReadmeComponentCommand;
import seungyong.helpmebackend.section.application.port.in.command.UpdateReadmeComponentCommand;
import seungyong.helpmebackend.section.domain.entity.Section;

import java.util.List;

public interface ReadmeComponentPortIn {
    List<Section> getComponents(Long userId, String owner, String name);

    Section createComponent(CreateReadmeComponentCommand command);

    Section updateComponent(UpdateReadmeComponentCommand command);

    void deleteComponent(DeleteReadmeComponentCommand command);
}
