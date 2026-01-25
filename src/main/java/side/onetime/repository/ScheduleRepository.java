package side.onetime.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import side.onetime.domain.Event;
import side.onetime.domain.Schedule;

import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule,Long> {

    Optional<List<Schedule>> findAllByEvent(Event event);

    Optional<List<Schedule>> findAllByEventAndDay(Event event, String day);

    Optional<List<Schedule>> findAllByEventAndDate(Event event, String date);

    @Query("SELECT s FROM Schedule s WHERE s.event.id IN :eventIds")
    List<Schedule> findAllByEventIdIn(@Param("eventIds") List<Long> eventIds);
}
