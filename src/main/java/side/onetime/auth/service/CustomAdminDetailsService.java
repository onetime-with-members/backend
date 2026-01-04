package side.onetime.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import side.onetime.auth.dto.CustomAdminDetails;
import side.onetime.domain.AdminUser;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.AdminErrorStatus;
import side.onetime.repository.AdminRepository;

@Service
@RequiredArgsConstructor
public class CustomAdminDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;

    /**
     * 관리자 이름으로 관리자 정보를 로드합니다.
     *
     * 데이터베이스에서 주어진 관리자 이름(username)을 기반으로 관리자를 조회하고,
     * CustomAdminDetails 객체로 래핑하여 반환합니다.
     *
     * @param username 관리자 이름
     * @return 관리자 상세 정보 (CustomAdminDetails 객체)
     * @throws CustomException 관리자 이름에 해당하는 관리자가 없을 경우 예외를 발생시킵니다.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser admin = adminRepository.findByName(username)
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));
        return new CustomAdminDetails(admin);
    }

    /**
     * 관리자 ID로 관리자 정보를 로드합니다.
     *
     * 데이터베이스에서 주어진 관리자 ID를 기반으로 관리자를 조회하고,
     * CustomAdminDetails 객체로 래핑하여 반환합니다.
     *
     * @param adminId 관리자 ID
     * @return 관리자 상세 정보 (CustomAdminDetails 객체)
     * @throws CustomException 관리자 ID에 해당하는 관리자가 없을 경우 예외를 발생시킵니다.
     */
    public UserDetails loadAdminByAdminId(Long adminId) throws UsernameNotFoundException {
        AdminUser admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));
        return new CustomAdminDetails(admin);
    }
}
