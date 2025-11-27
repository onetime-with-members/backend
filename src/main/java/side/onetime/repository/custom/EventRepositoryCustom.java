package side.onetime.repository.custom;

import org.springframework.data.domain.Pageable;
import side.onetime.domain.Event;

import java.util.List;

public interface EventRepositoryCustom {

    void deleteEvent(Event event);

    void deleteSchedulesByRanges(Event event, List<String> ranges);

    void deleteSchedulesByTimes(Event event, List<String> times);

    List<Event> findAllWithSort(Pageable pageable, String keyword, String sorting);
}
