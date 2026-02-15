package side.onetime.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import side.onetime.domain.BarBannerStaging;

public interface BarBannerStagingRepository extends JpaRepository<BarBannerStaging, Long> {
    long countByIsImportedFalse();
}
