package side.onetime.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import side.onetime.domain.GuideViewLog;
import side.onetime.domain.User;
import side.onetime.domain.enums.GuideType;

import java.util.Optional;

public interface GuideViewLogRepository extends JpaRepository<GuideViewLog, Long> {

    boolean existsByUserAndGuideType(User user, GuideType guideType);

    Optional<GuideViewLog> findByUser(User user);
}
