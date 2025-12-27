package side.onetime.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import side.onetime.domain.Event;
import side.onetime.domain.User;
import side.onetime.repository.custom.UserRepositoryCustom;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {
    Optional<User> findByName(String name);

    User findByProviderId(String providerId);

    boolean existsByProviderId(String providerId);

    void withdraw(User user);

    @Query("""
        SELECT DISTINCT u FROM User u
        JOIN FETCH u.selections s
        JOIN FETCH s.schedule sch
        JOIN FETCH sch.event e
        WHERE u.id IN :userIds
        AND e = :event
    """)
    List<User> findAllWithSelectionsAndSchedulesByEventAndUserIds(@Param("event") Event event, @Param("userIds") List<Long> userIds);
}
