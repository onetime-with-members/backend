package side.onetime.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import side.onetime.domain.Banner;
import side.onetime.domain.BarBanner;

import java.util.List;
import java.util.Optional;

public interface BarBannerRepository extends JpaRepository<BarBanner, Long> {

    Optional<BarBanner> findByIdAndIsDeletedFalse(Long id);

    List<BarBanner> findAllByIsDeletedFalseOrderByCreatedDateDesc(Pageable pageable);

    List<BarBanner> findAllByIsActivatedTrueAndIsDeletedFalse();

    long countByIsDeletedFalse();

    List<BarBanner> findAllByIsDeletedFalse();

    Optional<BarBanner> findByBarBannerStagingIdAndIsDeletedFalse(Long barBannerStagingId);
}
