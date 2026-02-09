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
import side.onetime.repository.custom.EmailLogRepositoryCustom;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long>, EmailLogRepositoryCustom {

    Page<EmailLog> findAllByOrderBySentAtDesc(Pageable pageable);

    Page<EmailLog> findByRecipientContainingOrderBySentAtDesc(String recipient, Pageable pageable);

    Page<EmailLog> findByStatusOrderBySentAtDesc(EmailLogStatus status, Pageable pageable);

    @Query("SELECT e.status, COUNT(e) FROM EmailLog e WHERE e.sentAt >= :since GROUP BY e.status")
    List<Object[]> countByStatusSince(@Param("since") LocalDateTime since);

    long countByStatus(EmailLogStatus status);
}
