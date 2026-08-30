package seungyong.helpmebackend.reflection.application.port.in;

import seungyong.helpmebackend.reflection.application.port.in.command.CreateReflectionCommand;
import seungyong.helpmebackend.reflection.application.port.in.command.ListReflectionsQuery;
import seungyong.helpmebackend.reflection.application.port.in.command.RegenerateReflectionCommand;
import seungyong.helpmebackend.reflection.application.port.in.command.SaveReflectionCommand;
import seungyong.helpmebackend.reflection.domain.entity.Reflection;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionGenerationResult;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionPage;

public interface ReflectionPortIn {
    ReflectionPage getReflections(ListReflectionsQuery query);

    Reflection getReflection(Long userId, Long projectId, Long reflectionId);

    ReflectionGenerationResult createReflection(CreateReflectionCommand command);

    Reflection saveReflection(SaveReflectionCommand command);

    ReflectionGenerationResult regenerateReflection(RegenerateReflectionCommand command);
}
