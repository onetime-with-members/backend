package side.onetime.repository.custom;

import org.springframework.data.domain.Pageable;
import side.onetime.domain.Event;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepositoryCustom {

    void deleteEvent(Event event);

    void deleteSchedulesByRanges(Event event, List<String> ranges);

    void deleteSchedulesByTimes(Event event, List<String> times);

    List<Event> findAllWithSort(Pageable pageable, String keyword, String sorting);

    List<Event> findAllWithFilters(Pageable pageable, String sortField, String sorting,
                                    String search, LocalDateTime startDate, LocalDateTime endDate);

    long countWithFilters(String search, LocalDateTime startDate, LocalDateTime endDate);
}
