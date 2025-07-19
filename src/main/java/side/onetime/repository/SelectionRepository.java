package side.onetime.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import side.onetime.domain.*;

import java.util.List;

public interface SelectionRepository extends JpaRepository<Selection, Long> {

    void deleteAllByMember(Member member);

    void deleteAllByUserAndScheduleIn(User user, List<Schedule> schedules);

    @Query("""
        SELECT s FROM Selection s
        JOIN FETCH s.schedule sc
        WHERE sc.event = :event
    """)
    List<Selection> findAllSelectionsByEvent(@Param("event") Event event);

    @Query("""
        SELECT COUNT(s) > 0 FROM Selection s
        WHERE s.user = :user
        AND s.schedule.event = :event
    """)
    boolean existsByUserAndEventSchedules(@Param("user") User user, @Param("event") Event event);

    @Query("""
        SELECT s FROM Selection s
        JOIN FETCH s.schedule sc
        WHERE s.member = :member
    """)
    List<Selection> findAllByMemberWithSchedule(@Param("member") Member member);

    @Query("""
        SELECT s FROM Selection s
        JOIN FETCH s.schedule sc
        JOIN FETCH sc.event
        WHERE s.user = :user
    """)
    List<Selection> findAllByUserWithScheduleAndEvent(@Param("user") User user);

    @Query("""
        SELECT s FROM Selection s
        JOIN FETCH s.schedule sc
        JOIN FETCH sc.event e
        WHERE e = :event AND (s.user.id IN :userIds OR s.member.id IN :memberIds)
    """)
    List<Selection> findAllByUserIdsOrMemberIdsWithScheduleAndEvent(@Param("event") Event event, @Param("userIds") List<Long> userIds, @Param("memberIds") List<Long> memberIds);
}
