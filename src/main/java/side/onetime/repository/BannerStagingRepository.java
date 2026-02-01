package side.onetime.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import side.onetime.domain.BannerStaging;

public interface BannerStagingRepository extends JpaRepository<BannerStaging, Long> {
}
