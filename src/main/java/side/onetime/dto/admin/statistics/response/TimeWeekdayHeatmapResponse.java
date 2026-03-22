package side.onetime.dto.admin.statistics.response;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Time × Weekday Heatmap Response
 * 이벤트 생성 시간대(0-23시) × 요일(월-일) 히트맵 데이터
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TimeWeekdayHeatmapResponse(
        List<String> hours,       // ["00", "01", ..., "23"]
        List<String> weekdays,    // ["Mon", "Tue", ..., "Sun"]
        List<List<Long>> data,    // data[hour][weekday] = count
        long maxValue,
        long totalEvents
) {
    public static TimeWeekdayHeatmapResponse of(
            List<String> hours,
            List<String> weekdays,
            List<List<Long>> data,
            long maxValue,
            long totalEvents
    ) {
        return new TimeWeekdayHeatmapResponse(hours, weekdays, data, maxValue, totalEvents);
    }
}
