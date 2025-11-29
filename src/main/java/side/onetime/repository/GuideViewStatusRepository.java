package side.onetime.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import side.onetime.domain.GuideViewStatus;
import side.onetime.domain.User;
import side.onetime.domain.enums.GuideType;

import java.util.Optional;

public interface GuideViewStatusRepository extends JpaRepository<GuideViewStatus, Long> {

    boolean existsByUserAndGuideType(User user, GuideType guideType);

    Optional<GuideViewStatus> findByUserAndIsViewedTrue(User user);
}
