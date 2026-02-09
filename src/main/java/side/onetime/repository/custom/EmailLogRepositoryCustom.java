package side.onetime.repository.custom;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;

import side.onetime.domain.EmailLog;

public interface EmailLogRepositoryCustom {

    List<EmailLog> findAllWithFilters(Pageable pageable, String search,
                                      LocalDateTime startDate, LocalDateTime endDate,
                                      String status, String targetGroup);

    long countWithFilters(String search, LocalDateTime startDate, LocalDateTime endDate,
                          String status, String targetGroup);
}
