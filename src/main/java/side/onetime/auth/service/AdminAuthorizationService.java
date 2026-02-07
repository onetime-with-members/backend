package side.onetime.auth.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import side.onetime.auth.dto.CustomAdminDetails;
import side.onetime.domain.enums.AdminStatus;

/**
 * 관리자 권한 체크를 위한 서비스.
 */
@Service("adminAuthorizationService")
public class AdminAuthorizationService {

    /**
     * 현재 인증된 사용자가 마스터 관리자인지 확인합니다.
     *
     * @return 마스터 관리자이면 true, 아니면 false
     */
    public boolean isMasterAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomAdminDetails adminDetails) {
            return AdminStatus.MASTER.equals(adminDetails.admin().getAdminStatus());
        }

        return false;
    }
}
