package side.onetime.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import side.onetime.domain.Event;
import side.onetime.domain.Member;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Boolean existsByEventAndName(Event event, String name);
    Optional<Member> findByEventAndNameAndPin(Event event, String name, String pin);
    Optional<Member> findByMemberId(UUID memberId);

    List<Member> findAllByEvent(Event event);

    @Query("SELECT m FROM Member m " +
            "JOIN FETCH m.selections s " +
            "JOIN FETCH s.schedule sch " +
            "WHERE m.event = :event AND m.name IN :names")
    List<Member> findAllWithSelectionsAndSchedulesByEventAndNames(@Param("event") Event event, @Param("names") List<String> names);

    List<Member> findAllByEventIdIn(List<Long> eventIds);
}
