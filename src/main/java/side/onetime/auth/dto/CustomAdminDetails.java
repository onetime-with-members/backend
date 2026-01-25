package side.onetime.auth.dto;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import side.onetime.domain.AdminUser;
import side.onetime.domain.enums.AdminStatus;

import java.util.Collection;
import java.util.Collections;

public record CustomAdminDetails(AdminUser admin) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (admin.getAdminStatus() == AdminStatus.PENDING_APPROVAL) {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_PENDING_APPROVAL"));
        }

        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Override
    public String getPassword() {
        return admin.getPassword();
    }

    @Override
    public String getUsername() {
        return admin.getName();
    }

    public Long getId() {
        return admin.getId();
    }
}
