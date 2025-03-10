package side.onetime.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import side.onetime.domain.Event;
import side.onetime.domain.EventParticipation;
import side.onetime.domain.User;

import java.util.List;

public interface EventParticipationRepository extends JpaRepository<EventParticipation,Long> {
    List<EventParticipation> findAllByEvent(Event event);
    List<EventParticipation> findAllByUser(User user);
    EventParticipation findByUserAndEvent(User user, Event event);
}
