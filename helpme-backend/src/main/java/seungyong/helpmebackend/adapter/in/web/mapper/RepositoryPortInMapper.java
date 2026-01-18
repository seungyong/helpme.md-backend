package seungyong.helpmebackend.adapter.in.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import seungyong.helpmebackend.adapter.in.web.dto.repository.response.ResponseRepository;
import seungyong.helpmebackend.adapter.out.persistence.mapper.EvaluationPortOutMapper;
import seungyong.helpmebackend.adapter.out.result.RepositoryDetailResult;
import seungyong.helpmebackend.domain.entity.component.Component;
import seungyong.helpmebackend.domain.entity.evaluation.Evaluation;

import java.util.List;

@Mapper
public interface RepositoryPortInMapper {
    RepositoryPortInMapper INSTANCE = Mappers.getMapper(RepositoryPortInMapper.class);

    @Mapping(target = "owner", source = "repositoryDetailResult.owner")
    @Mapping(target = "name", source = "repositoryDetailResult.name")
    @Mapping(target = "avatarUrl", source = "repositoryDetailResult.avatarUrl")
    @Mapping(target = "defaultBranch", source = "repositoryDetailResult.defaultBranch")
    @Mapping(target = "evaluation", source = "evaluation")
    @Mapping(target = "components", source = "components")
    @Mapping(target = "branches", source = "branches")
    @Mapping(target = "content", source = "content")
    ResponseRepository toResponseRepository(
            RepositoryDetailResult repositoryDetailResult,
            ResponseRepository.Evaluation evaluation,
            List<ResponseRepository.Component> components,
            String[] branches,
            String content
    );


    @Mapping(target = "status", expression = "java(evaluation.getStatusMessage())")
    @Mapping(target = "rating", source = "rating")
    @Mapping(target = "content", expression = "java(evaluation.getContentToArray())")
    ResponseRepository.Evaluation toResponseEvaluation(Evaluation evaluation);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "content", source = "content")
    ResponseRepository.Component toResponseComponent(Component component);
}
