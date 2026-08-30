package seungyong.helpmebackend.reflection.application.port.out;

import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionSourceSnapshot;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;

import java.time.LocalDate;

public interface ReflectionGenerationPortOut {
    /**
     * 주어진 프로젝트와 회고 종류, 기간, 소스 스냅샷을 기반으로 회고를 생성합니다.
     *
     * @param project      회고를 생성할 프로젝트
     * @param kind         회고의 종류 (일일, 주간 등)
     * @param periodStart  회고 기간의 시작 날짜
     * @param periodEnd    회고 기간의 종료 날짜
     * @param source       회고 생성을 위한 소스 스냅샷
     * @return 생성된 회고의 제목과 내용
     */
    GeneratedReflection generate(
            Project project,
            ReflectionKind kind,
            LocalDate periodStart,
            LocalDate periodEnd,
            ReflectionSourceSnapshot source
    );

    record GeneratedReflection(String title, ReflectionDocument content) {
    }
}
