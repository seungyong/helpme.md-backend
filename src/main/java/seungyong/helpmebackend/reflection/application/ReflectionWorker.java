package seungyong.helpmebackend.reflection.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import seungyong.helpmebackend.project.application.port.out.ProjectPortOut;
import seungyong.helpmebackend.project.domain.entity.Project;
import seungyong.helpmebackend.reflection.application.port.out.ReflectionGenerationPortOut;
import seungyong.helpmebackend.reflection.application.port.out.ReflectionPortOut;
import seungyong.helpmebackend.reflection.domain.entity.Reflection;
import seungyong.helpmebackend.reflection.domain.exception.ReflectionErrorCode;
import seungyong.helpmebackend.reflection.domain.exception.ReflectionGenerationException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReflectionWorker {
    private static final Duration STUCK_AFTER = Duration.ofMinutes(5);

    private final ReflectionPortOut reflectionPortOut;
    private final ProjectPortOut projectPortOut;
    private final ReflectionGenerationPortOut generationPortOut;
    private final ReflectionSourceBuilder sourceBuilder;

    /**
     * 주기적으로 Reflection을 처리합니다.
     * - Reflection을 가져와서 처리
     * - 처리 중 예외 발생 시 실패 처리
     * - Reflection이 없으면 아무 작업도 수행하지 않음
     */
    @Scheduled(fixedDelayString = "${workers.reflection.fixed-delay-ms:1000}")
    public void runOnce() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        reflectionPortOut.claimNext(now, now.minus(STUCK_AFTER))
                .ifPresent(this::process);
    }

    /**
     * Reflection을 처리합니다.
     * - 프로젝트를 가져와서 ReflectionSource를 생성
     * - ReflectionSource가 없으면 예외 발생
     * - Reflection을 생성하고 완료 처리
     * - 예외 발생 시 실패 처리
     *
     * @param reflection 처리할 Reflection 엔티티
     */
    private void process(Reflection reflection) {
        try {
            Project project = projectPortOut.getById(reflection.projectId()).orElseThrow();
            // AI 호출 직전 DB 근거 재구성, 생성 요청 이후 추가된 Activity·Devlog까지 반영
            ReflectionSourceBuilder.Result source = sourceBuilder.build(
                    project,
                    reflection.kind(),
                    reflection.periodStart(),
                    reflection.periodEnd()
            );
            if (!source.hasSource()) {
                throw new IllegalStateException("reflection source disappeared");
            }
            ReflectionGenerationPortOut.GeneratedReflection generated =
                    generationPortOut.generate(
                            project,
                            reflection.kind(),
                            reflection.periodStart(),
                            reflection.periodEnd(),
                            source.snapshot()
                    );
            // AI 성공 시 content와 실제 사용한 snapshot/hash를 함께 교체, 결과와 근거의 불일치 방지
            reflectionPortOut.completeGeneration(
                    reflection.id(),
                    generated.title(),
                    generated.content(),
                    source.quality(),
                    source.snapshot(),
                    source.sourceHash(),
                    OffsetDateTime.now(ZoneOffset.UTC)
            );
        } catch (ReflectionGenerationException exception) {
            reflectionPortOut.failGeneration(
                    reflection.id(), exception.getErrorCode(), exception.getMessage()
            );
            log.warn(
                    "Reflection generation failed: reflectionId={}, attempts={}, code={}",
                    reflection.id(), reflection.generationAttempts(), exception.getErrorCode()
            );
        } catch (RuntimeException exception) {
            ReflectionErrorCode code = ReflectionErrorCode.REFLECTION_GENERATION_FAILED;
            reflectionPortOut.failGeneration(
                    reflection.id(), code.getErrorCode(), code.getMessage()
            );
            log.warn(
                    "Reflection generation failed: reflectionId={}, attempts={}, exceptionType={}",
                    reflection.id(), reflection.generationAttempts(),
                    exception.getClass().getSimpleName()
            );
        }
    }
}
