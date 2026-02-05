package side.onetime.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import side.onetime.domain.EventConfirmation;

public interface EventConfirmationRepository extends JpaRepository<EventConfirmation, Long> {
    Optional<EventConfirmation> findByEventId(Long eventId);
}
