package seungyong.helpmebackend.infrastructure.mapper;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Setter
@Getter
public class CustomTimeStamp {
    private LocalDateTime timestamp;

    public CustomTimeStamp() {
        this.timestamp = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public CustomTimeStamp(LocalDateTime timestamp) {
        this.timestamp = timestamp.atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime();
    }

    @Override
    public String toString() {
        return this.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
