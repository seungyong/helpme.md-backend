package seungyong.helpmebackend.devlog.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import seungyong.helpmebackend.devlog.application.port.in.DevlogPortIn;
import seungyong.helpmebackend.devlog.application.port.in.command.SaveDevlogCommand;
import seungyong.helpmebackend.devlog.application.port.out.DevlogPortOut;
import seungyong.helpmebackend.devlog.domain.entity.Devlog;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.DocumentErrorCode;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;
import seungyong.helpmebackend.project.application.ProjectAccessResolver;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DevlogService implements DevlogPortIn {
    private final ProjectAccessResolver projectAccessResolver;
    private final DevlogPortOut devlogPortOut;

    @Override
    public Devlog getDevlog(Long userId, Long projectId, LocalDate logDate) {
        validateIdentityAndDate(userId, projectId, logDate);
        projectAccessResolver.resolveActive(userId, projectId);
        return devlogPortOut.getByProjectIdAndLogDate(projectId, logDate)
                .orElseGet(() -> Devlog.empty(projectId, logDate));
    }

    @Override
    public Devlog saveDevlog(SaveDevlogCommand command) {
        validateCommand(command);
        projectAccessResolver.resolveActive(command.userId(), command.projectId());

        Optional<Devlog> current = devlogPortOut.getByProjectIdAndLogDate(
                command.projectId(), command.logDate()
        );

        // 내용이 비어있으면 삭제 처리, 아니면 생성/업데이트 처리
        if (!StringUtils.hasText(command.contentMarkdown())) {
            return delete(command, current);
        }

        if (current.isEmpty()) {
            requireFirstCreation(command.version());
            // 새로운 개발로그 생성 (버전이 없는 상태)
            return devlogPortOut.create(
                    command.projectId(), command.logDate(), command.contentMarkdown()
            );
        }

        requireCurrentVersion(current.orElseThrow(), command.version());
        return devlogPortOut.updateIfVersionMatches(
                        command.projectId(),
                        command.logDate(),
                        command.contentMarkdown(),
                        command.version(),
                        OffsetDateTime.now(ZoneOffset.UTC)
                )
                .orElseThrow(DevlogService::versionConflict);
    }

    private Devlog delete(SaveDevlogCommand command, Optional<Devlog> current) {
        if (current.isEmpty()) {
            requireFirstCreation(command.version());
            return Devlog.empty(command.projectId(), command.logDate());
        }

        requireCurrentVersion(current.orElseThrow(), command.version());
        if (!devlogPortOut.deleteIfVersionMatches(
                command.projectId(), command.logDate(), command.version()
        )) {
            throw versionConflict();
        }
        return Devlog.empty(command.projectId(), command.logDate());
    }

    private void validateCommand(SaveDevlogCommand command) {
        if (command == null || command.contentMarkdown() == null) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
        validateIdentityAndDate(command.userId(), command.projectId(), command.logDate());
        if (command.version() != null && command.version() < 0) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private void validateIdentityAndDate(Long userId, Long projectId, LocalDate logDate) {
        if (userId == null || projectId == null || logDate == null) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private void requireFirstCreation(Integer version) {
        if (version != null) {
            throw versionConflict();
        }
    }

    private void requireCurrentVersion(Devlog current, Integer requestedVersion) {
        if (!current.version().equals(requestedVersion)) {
            throw versionConflict();
        }
    }

    private static CustomException versionConflict() {
        return new CustomException(DocumentErrorCode.DOCUMENT_VERSION_CONFLICT);
    }
}
