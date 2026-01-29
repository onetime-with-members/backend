package side.onetime.repository.custom;

import org.springframework.data.domain.Pageable;
import side.onetime.domain.User;

import java.time.LocalDateTime;
import java.util.List;

public interface UserRepositoryCustom {

    void withdraw(User user);

    List<User> findAllWithSort(Pageable pageable, String keyword, String sorting);

    List<User> findAllWithFilters(Pageable pageable, String sortField, String sorting,
                                   String search, LocalDateTime startDate, LocalDateTime endDate);

    long countWithFilters(String search, LocalDateTime startDate, LocalDateTime endDate);
}
