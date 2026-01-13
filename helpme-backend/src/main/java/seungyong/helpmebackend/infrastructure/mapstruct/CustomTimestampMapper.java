package seungyong.helpmebackend.infrastructure.mapstruct;

import org.mapstruct.Mapper;
import seungyong.helpmebackend.infrastructure.mapstruct.annotation.MapCreatedTime;
import seungyong.helpmebackend.domain.mapper.CustomTimeStamp;

import java.time.LocalDateTime;

@Mapper
public class CustomTimestampMapper {
    @MapCreatedTime
    public String formatCreatedTime(LocalDateTime dateTime) {
        return new CustomTimeStamp(dateTime).toString();
    }
}
