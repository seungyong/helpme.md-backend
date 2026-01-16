package seungyong.helpmebackend.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import seungyong.helpmebackend.adapter.out.persistence.entity.EvaluationJpaEntity;
import seungyong.helpmebackend.domain.entity.evaluation.Evaluation;

@Mapper
public interface EvaluationPortOutMapper {
    EvaluationPortOutMapper INSTANCE = Mappers.getMapper(EvaluationPortOutMapper.class);

    @Mapping(target = "uploader.id", source = "uploaderId")
    @Mapping(target = "repoFullName", source = "repoFullName")
    @Mapping(target = "rating", source = "rating")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "status", source = "status")
    EvaluationJpaEntity toEntity(Evaluation evaluation);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "uploaderId", source = "uploader.id")
    @Mapping(target = "repoFullName", source = "repoFullName")
    @Mapping(target = "rating", source = "rating")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "updatedAt", source = "updatedAt")
    Evaluation toDomain(EvaluationJpaEntity entity);
}
