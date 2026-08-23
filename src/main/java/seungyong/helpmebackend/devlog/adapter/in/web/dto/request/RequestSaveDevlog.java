package seungyong.helpmebackend.devlog.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotNull;
import seungyong.helpmebackend.devlog.application.port.in.command.SaveDevlogCommand;

import java.time.LocalDate;

public record RequestSaveDevlog(
        @NotNull(message = "contentMd는 필수입니다.")
        String contentMd,
        Integer version
) {
    public SaveDevlogCommand toCommand(Long userId, Long projectId, LocalDate logDate) {
        return new SaveDevlogCommand(userId, projectId, logDate, contentMd, version);
    }
}
