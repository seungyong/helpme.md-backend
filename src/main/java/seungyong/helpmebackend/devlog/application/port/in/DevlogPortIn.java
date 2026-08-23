package seungyong.helpmebackend.devlog.application.port.in;

import seungyong.helpmebackend.devlog.application.port.in.command.SaveDevlogCommand;
import seungyong.helpmebackend.devlog.domain.entity.Devlog;

import java.time.LocalDate;

public interface DevlogPortIn {
    Devlog getDevlog(Long userId, Long projectId, LocalDate logDate);

    Devlog saveDevlog(SaveDevlogCommand command);
}
