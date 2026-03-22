package side.onetime.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import side.onetime.domain.EmailTemplate;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    List<EmailTemplate> findAllByOrderByUpdatedDateDesc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    java.util.Optional<EmailTemplate> findByCode(String code);
}
