package side.onetime.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import side.onetime.domain.GuideViewStatus;
import side.onetime.domain.User;
import side.onetime.domain.enums.GuideType;

public interface GuideViewStatusRepository extends JpaRepository<GuideViewStatus, Long> {

    boolean existsByUserAndGuideType(User user, GuideType guideType);
}
