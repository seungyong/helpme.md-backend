package seungyong.helpmebackend.adapter.in.web.mapper;

import org.mapstruct.Mapper;
import seungyong.helpmebackend.common.annotation.mapper.MapCreatedTime;
import seungyong.helpmebackend.infrastructure.mapper.CustomTimeStamp;

import java.time.LocalDateTime;

@Mapper
public class CustomTimestampMapper {
    @MapCreatedTime
    public String formatCreatedTime(LocalDateTime dateTime) {
        return new CustomTimeStamp(dateTime).toString();
    }
}
