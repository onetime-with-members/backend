package side.onetime.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import side.onetime.domain.EmailSchedule;
import side.onetime.domain.enums.EmailScheduleStatus;

public interface EmailScheduleRepository extends JpaRepository<EmailSchedule, Long> {

    @Query("SELECT es FROM EmailSchedule es LEFT JOIN FETCH es.template ORDER BY es.scheduledAt DESC")
    List<EmailSchedule> findAllByOrderByScheduledAtDesc();

    List<EmailSchedule> findByStatusAndScheduledAtLessThanEqual(
            EmailScheduleStatus status, LocalDateTime now);
}
