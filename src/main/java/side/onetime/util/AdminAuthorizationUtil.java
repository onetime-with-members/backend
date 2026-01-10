package side.onetime.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import side.onetime.auth.dto.CustomAdminDetails;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.AdminErrorStatus;
import side.onetime.global.common.status.ErrorStatus;

import java.util.Optional;

public class AdminAuthorizationUtil {

    private AdminAuthorizationUtil() {
        throw new AssertionError();
    }

    /**
     * 현재 로그인한 관리자의 ID를 반환하는 메서드.
     *
     * SecurityContextHolder에서 Authentication을 가져와
     * CustomAdminDetails로 캐스팅한 후, 관리자 ID를 추출합니다.
     *
     * @return 로그인된 관리자의 ID
     */
    public static Long getLoginAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = Optional.ofNullable(authentication)
                .map(Authentication::getPrincipal)
                .orElseThrow(() -> new CustomException(ErrorStatus._UNAUTHORIZED));

        if (!(principal instanceof CustomAdminDetails adminDetails)) {
            throw new CustomException(AdminErrorStatus._UNAUTHORIZED);
        }
        return adminDetails.getId();
    }
}
