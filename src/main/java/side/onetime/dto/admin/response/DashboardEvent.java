package side.onetime.dto.admin.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import side.onetime.domain.Event;
import side.onetime.domain.Schedule;
import side.onetime.domain.enums.Category;
import side.onetime.util.DateUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DashboardEvent(
        Long id,
        String eventId,
        String title,
        String startTime,
        String endTime,
        Category category,
        int participantCount,
        String createdDate,
        List<String> ranges,
        String dateRange,
        String timeRange
) {
    public static DashboardEvent of(Event event, List<Schedule> schedules, int participantCount) {
        String dateRange;
        List<String> ranges;

        if (event.getCategory() == Category.DATE) {
            // 날짜형: yyyy.MM.dd -> MM/dd(E) 형식으로 변환
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MM/dd(E)", Locale.KOREAN);

            List<LocalDate> sortedDates = schedules.stream()
                    .map(Schedule::getDate)
                    .filter(Objects::nonNull)
                    .map(dateStr -> {
                        try {
                            return LocalDate.parse(dateStr, inputFormatter);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .sorted()
                    .distinct()
                    .toList();

            ranges = sortedDates.stream()
                    .map(d -> d.format(outputFormatter))
                    .toList();

            if (sortedDates.isEmpty()) {
                dateRange = "-";
            } else if (sortedDates.size() == 1) {
                dateRange = sortedDates.get(0).format(outputFormatter);
            } else {
                dateRange = sortedDates.get(0).format(outputFormatter) + " - " +
                           sortedDates.get(sortedDates.size() - 1).format(outputFormatter);
            }
        } else {
            // 요일형
            ranges = DateUtil.getSortedDayRanges(schedules.stream().map(Schedule::getDay).toList());
            if (ranges.isEmpty()) {
                dateRange = "-";
            } else if (ranges.size() == 1) {
                dateRange = ranges.get(0);
            } else {
                dateRange = ranges.get(0) + " - " + ranges.get(ranges.size() - 1);
            }
        }

        String timeRange = event.getStartTime() + " - " + event.getEndTime();

        return new DashboardEvent(
                event.getId(),
                String.valueOf(event.getEventId()),
                event.getTitle(),
                event.getStartTime(),
                event.getEndTime(),
                event.getCategory(),
                participantCount,
                event.getCreatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd (E) a hh:mm", Locale.KOREAN)),
                ranges,
                dateRange,
                timeRange
        );
    }
}
