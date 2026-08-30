package seungyong.helpmebackend.reflection.application.port.out;

import seungyong.helpmebackend.reflection.domain.entity.Reflection;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionDocument;
import seungyong.helpmebackend.reflection.domain.entity.ReflectionSourceSnapshot;
import seungyong.helpmebackend.reflection.domain.type.ReflectionKind;
import seungyong.helpmebackend.reflection.domain.type.ReflectionStatus;
import seungyong.helpmebackend.reflection.domain.type.SourceQuality;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ReflectionPortOut {
    /**
     * 회고를 생성합니다. 이미 존재하는 경우에는 기존 회고를 반환합니다.
     * @param reflection 생성할 회고 엔티티
     * @return 생성 결과 (생성 여부와 회고 엔티티)
     */
    CreateResult createIfAbsent(Reflection reflection);

    /**
     * 특정 프로젝트와 회고 ID에 해당하는 회고를 조회합니다.
     * @param projectId 프로젝트 ID
     * @param reflectionId 회고 ID
     * @return 회고 엔티티 (존재하지 않으면 Optional.empty())
     */
    Optional<Reflection> getByProjectIdAndId(Long projectId, Long reflectionId);

    /**
     * 특정 프로젝트, 회고 종류, 회고 시작일에 해당하는 회고를 조회합니다.
     * @param projectId 프로젝트 ID
     * @param kind 회고 종류
     * @param periodStart 회고 시작일
     * @return 회고 엔티티 (존재하지 않으면 Optional.empty())
     */
    Optional<Reflection> getByPeriod(
            Long projectId, ReflectionKind kind, LocalDate periodStart
    );

    /**
     * 회고 목록을 조회합니다.
     * @param projectId 프로젝트 ID
     * @param kind 회고 종류
     * @param from 조회 시작일 (null이면 제한 없음)
     * @param to 조회 종료일 (null이면 제한 없음)
     * @param status 회고 상태 (null이면 제한 없음)
     * @param cursorPeriodStart 커서 기준 회고 시작일 (null이면 제한 없음)
     * @param cursorId 커서 기준 회고 ID (null이면 제한 없음)
     * @param limit 조회할 회고 수
     * @return 회고 목록
     */
    List<Reflection> findPage(
            Long projectId,
            ReflectionKind kind,
            LocalDate from,
            LocalDate to,
            ReflectionStatus status,
            LocalDate cursorPeriodStart,
            Long cursorId,
            int limit
    );

    /**
     * 특정 프로젝트의 저장된 일일 회고 목록을 조회합니다.
     * @param projectId 프로젝트 ID
     * @param from 조회 시작일
     * @param to 조회 종료일
     * @return 저장된 일일 회고 목록
     */
    List<Reflection> findSavedDaily(
            Long projectId, LocalDate from, LocalDate to
    );

    /**
     * 회고를 저장합니다. 버전이 일치하지 않으면 저장하지 않고 Optional.empty()를 반환합니다.
     * @param projectId 프로젝트 ID
     * @param reflectionId 회고 ID
     * @param title 회고 제목
     * @param content 회고 내용
     * @param expectedVersion 기대하는 버전
     * @param savedAt 저장 시각
     * @return 저장된 회고 엔티티 (버전 불일치 시 Optional.empty())
     */
    Optional<Reflection> saveIfVersionMatches(
            Long projectId,
            Long reflectionId,
            String title,
            ReflectionDocument content,
            int expectedVersion,
            OffsetDateTime savedAt
    );

    /**
     * 회고를 재생성 대기열에 추가합니다. 이미 대기열에 있거나 생성 중인 경우에는 상태를 변경하지 않습니다.
     * @param projectId 프로젝트 ID
     * @param reflectionId 회고 ID
     * @return 재생성 대기열에 추가된 회고 엔티티 (존재하지 않으면 Optional.empty())
     */
    Optional<Reflection> queueRegeneration(
            Long projectId,
            Long reflectionId
    );

    /**
     * 다음 회고 생성 작업을 claim합니다. 이미 생성 중인 작업이 오래 걸린 경우에는 재시도 횟수를 확인하고 실패 처리하거나 재시도합니다.
     * @param now 현재 시각
     * @param stuckBefore 오래 걸린 작업으로 간주할 시각
     * @return claim된 회고 엔티티 (없으면 Optional.empty())
     */
    Optional<Reflection> claimNext(OffsetDateTime now, OffsetDateTime stuckBefore);

    /**
     * 회고 생성 작업을 완료합니다. 생성된 회고의 제목, 내용, 근거, 품질, 해시, 생성 시각을 업데이트합니다.
     * @param reflectionId 회고 ID
     * @param title 회고 제목
     * @param content 회고 내용
     * @param sourceQuality 근거 품질
     * @param sourceSnapshot 근거 스냅샷
     * @param sourceHash 근거 해시
     * @param generatedAt 생성 시각
     */
    void completeGeneration(
            Long reflectionId,
            String title,
            ReflectionDocument content,
            SourceQuality sourceQuality,
            ReflectionSourceSnapshot sourceSnapshot,
            String sourceHash,
            OffsetDateTime generatedAt
    );

    /**
     * 회고 생성 작업을 실패 처리합니다. 에러 코드와 메시지를 업데이트합니다.
     * @param reflectionId 회고 ID
     * @param errorCode 에러 코드
     * @param errorMessage 에러 메시지
     */
    void failGeneration(Long reflectionId, String errorCode, String errorMessage);

    record CreateResult(Reflection reflection, boolean created) {
    }
}
