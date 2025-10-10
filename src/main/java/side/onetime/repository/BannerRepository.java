package side.onetime.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import side.onetime.domain.Banner;

import java.util.List;
import java.util.Optional;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    Optional<Banner> findByIdAndIsDeletedFalse(Long id);

    List<Banner> findAllByIsDeletedFalseOrderByCreatedDateDesc(Pageable pageable);

    List<Banner> findAllByIsActivatedTrueAndIsDeletedFalse();

    long countByIsDeletedFalse();

    @Modifying
    @Query("""
        UPDATE Banner b
            SET b.clickCount = b.clickCount + 1
        WHERE b.id = :id
            AND b.isActivated = true
            AND b.isDeleted = false
    """)
    void increaseClickCount(@Param("id") Long id);
}
