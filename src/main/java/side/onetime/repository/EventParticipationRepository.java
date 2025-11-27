package side.onetime.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import side.onetime.domain.Event;
import side.onetime.domain.EventParticipation;
import side.onetime.domain.User;
import side.onetime.repository.custom.EventParticipationRepositoryCustom;

import java.util.List;

public interface EventParticipationRepository extends JpaRepository<EventParticipation,Long>, EventParticipationRepositoryCustom {

    List<EventParticipation> findAllByEvent(Event event);

    @Query("""
        SELECT ep FROM EventParticipation ep
        JOIN FETCH ep.event
        WHERE ep.user = :user
    """)
    List<EventParticipation> findAllByUserWithEvent(@Param("user") User user);

    @Query("""
        SELECT ep FROM EventParticipation ep
        JOIN FETCH ep.event e
        LEFT JOIN FETCH e.members
        LEFT JOIN FETCH ep.user
        WHERE ep.event = :event
    """)
    List<EventParticipation> findAllByEventWithEventAndMemberAndUser(@Param("event") Event event);

    EventParticipation findByUserAndEvent(User user, Event event);

    List<EventParticipation> findAllByEventIdIn(List<Long> eventIds);
}
