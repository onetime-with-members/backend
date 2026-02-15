package side.onetime.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import side.onetime.domain.BannerStaging;

public interface BannerStagingRepository extends JpaRepository<BannerStaging, Long> {
    long countByIsImportedFalse();
}
