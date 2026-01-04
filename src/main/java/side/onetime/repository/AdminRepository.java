package side.onetime.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import side.onetime.domain.AdminUser;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<AdminUser, Long> {

    boolean existsAdminUsersByEmail(String email);

    Optional<AdminUser> findAdminUserByEmail(String email);

    Optional<AdminUser> findByName(String name);
}
