package side.onetime.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import side.onetime.auth.dto.CustomAdminDetails;
import side.onetime.domain.AdminUser;
import side.onetime.domain.enums.AdminStatus;

public abstract class AdminControllerTestConfig extends ControllerTestConfig {

    @BeforeEach
    void setAdminAuthentication() {
        AdminUser mockAdmin = AdminUser.builder().name("testAdmin").email("admin@example.com").build();
        mockAdmin.updateAdminStatus(AdminStatus.MASTER);

        CustomAdminDetails customAdminDetails = new CustomAdminDetails(mockAdmin);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(customAdminDetails, null, customAdminDetails.getAuthorities())
        );
    }
}
