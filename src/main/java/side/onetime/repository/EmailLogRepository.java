package side.onetime.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import side.onetime.domain.EmailLog;
import side.onetime.domain.enums.EmailLogStatus;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    Page<EmailLog> findAllByOrderBySentAtDesc(Pageable pageable);

    Page<EmailLog> findByRecipientContainingOrderBySentAtDesc(String recipient, Pageable pageable);

    Page<EmailLog> findByStatusOrderBySentAtDesc(EmailLogStatus status, Pageable pageable);

    Page<EmailLog> findByTargetGroupOrderBySentAtDesc(String targetGroup, Pageable pageable);

    @Query("SELECT e FROM EmailLog e WHERE e.sentAt BETWEEN :startDate AND :endDate ORDER BY e.sentAt DESC")
    Page<EmailLog> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT e.status, COUNT(e) FROM EmailLog e WHERE e.sentAt >= :since GROUP BY e.status")
    List<Object[]> countByStatusSince(@Param("since") LocalDateTime since);

    long countByStatus(EmailLogStatus status);

    long countBySentAtAfter(LocalDateTime since);
}
