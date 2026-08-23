package seungyong.helpmebackend.devlog.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;
import seungyong.helpmebackend.devlog.domain.entity.Devlog;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ResponseDevlog(
        boolean exists,
        Long id,
        Long projectId,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate logDate,
        String contentMd,
        Integer version,
        OffsetDateTime updatedAt
) {
    public static ResponseDevlog from(Devlog devlog) {
        return new ResponseDevlog(
                devlog.exists(),
                devlog.id(),
                devlog.projectId(),
                devlog.logDate(),
                devlog.contentMarkdown(),
                devlog.version(),
                devlog.updatedAt()
        );
    }
}
