package side.onetime.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import side.onetime.auth.dto.CustomUserDetails;
import side.onetime.domain.User;

public abstract class UserControllerTestConfig extends ControllerTestConfig {

    @BeforeEach
    void setUserAuthentication() {
        User mockUser = User.builder().nickname("testUser").email("test@example.com").build();

        CustomUserDetails customUserDetails = new CustomUserDetails(mockUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities())
        );
    }
}
