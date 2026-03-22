package side.onetime.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import side.onetime.domain.AdminUser;
import side.onetime.domain.Event;
import side.onetime.domain.RefreshToken;
import side.onetime.domain.Schedule;
import side.onetime.domain.User;
import side.onetime.domain.enums.AdminStatus;
import side.onetime.domain.enums.ParticipationRole;
import side.onetime.dto.admin.request.LoginAdminUserRequest;
import side.onetime.dto.admin.request.RegisterAdminUserRequest;
import side.onetime.dto.admin.request.UpdateAdminUserStatusRequest;
import side.onetime.dto.admin.response.AdminUserDetailResponse;
import side.onetime.dto.admin.response.DashboardEvent;
import side.onetime.dto.admin.response.DashboardUser;
import side.onetime.dto.admin.response.GetAdminUserProfileResponse;
import side.onetime.dto.admin.response.GetAllDashboardEventsResponse;
import side.onetime.dto.admin.response.GetAllDashboardUsersResponse;
import side.onetime.dto.admin.response.LoginAdminUserResponse;
import side.onetime.dto.admin.response.PageInfo;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.AdminErrorStatus;
import side.onetime.repository.AdminRepository;
import side.onetime.repository.EventParticipationRepository;
import side.onetime.repository.EventRepository;
import side.onetime.repository.RefreshTokenRepository;
import side.onetime.repository.ScheduleRepository;
import side.onetime.repository.StatisticsRepository;
import side.onetime.repository.UserRepository;
import side.onetime.util.AdminAuthorizationUtil;
import side.onetime.util.JwtUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final EventRepository eventRepository;
    private final EventParticipationRepository eventParticipationRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final StatisticsRepository statisticsRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 관리자 계정 등록 메서드.
     *
     * 입력된 이름, 이메일, 비밀번호 정보를 바탕으로 새로운 관리자 계정을 생성합니다.
     * 생성된 계정은 기본적으로 승인 대기 상태(PENDING_APPROVAL)로 저장됩니다.
     *
     * @param request 관리자 이름, 이메일, 비밀번호 정보를 담은 요청 객체
     */
    @Transactional
    public void registerAdminUser(RegisterAdminUserRequest request) {

        if (adminRepository.existsAdminUsersByEmail(request.email())) {
            throw new CustomException(AdminErrorStatus._IS_DUPLICATED_EMAIL);
        }
        AdminUser newAdminUser = request.toEntity(passwordEncoder.encode(request.password()));
        adminRepository.save(newAdminUser);
    }

    /**
     * 관리자 계정 로그인 메서드.
     *
     * 입력된 이메일과 비밀번호를 기반으로 관리자 계정 로그인을 시도합니다.
     * - 이메일이 존재하지 않으면 예외가 발생합니다.
     * - 계정이 승인 대기 상태인 경우 예외가 발생합니다.
     * - 비밀번호가 일치하지 않으면 예외가 발생합니다.
     *
     * @param request 로그인 요청 정보 (이메일, 비밀번호)
     * @param browserId 브라우저 식별자 (User-Agent 해시)
     * @param userIp 클라이언트 IP
     * @param userAgent User-Agent 문자열
     */
    @Transactional
    public LoginAdminUserResponse loginAdminUser(LoginAdminUserRequest request, String browserId, String userIp, String userAgent) {

        AdminUser adminUser = adminRepository.findAdminUserByEmail(request.email())
                        .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));
        if (AdminStatus.PENDING_APPROVAL == adminUser.getAdminStatus()) {
            throw new CustomException(AdminErrorStatus._IS_NOT_APPROVED_ADMIN_USER);
        }
        if (!passwordEncoder.matches(request.password(), adminUser.getPassword())) {
            throw new CustomException(AdminErrorStatus._IS_NOT_EQUAL_PASSWORD);
        }

        Long adminId = adminUser.getId();

        // 기존 브라우저의 ACTIVE 토큰 revoke
        refreshTokenRepository.revokeByUserIdAndBrowserId(adminId, "ADMIN", browserId);

        // 새 토큰 생성
        String jti = UUID.randomUUID().toString();
        String accessToken = jwtUtil.generateAccessToken(adminId, "ADMIN");
        String refreshTokenValue = jwtUtil.generateRefreshToken(adminId, "ADMIN", browserId, jti);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryAt = jwtUtil.calculateRefreshTokenExpiryAt(now);

        RefreshToken refreshToken = RefreshToken.create(
                adminId, "ADMIN", jti, browserId, refreshTokenValue,
                now, expiryAt, userIp, userAgent
        );
        refreshTokenRepository.save(refreshToken);

        return LoginAdminUserResponse.of(accessToken, refreshTokenValue);
    }

    /**
     * 관리자 프로필 조회 메서드.
     *
     * Authorization 헤더에서 액세스 토큰을 추출하고, 토큰에 포함된 ID를 기반으로
     * 관리자 정보를 조회합니다.
     * - 토큰이 유효하지 않거나 관리자 정보가 존재하지 않을 경우 예외가 발생합니다.
     *
     * @return 관리자 프로필 응답 객체
     */
    @Transactional(readOnly = true)
    public GetAdminUserProfileResponse getAdminUserProfile() {

        AdminUser adminUser = adminRepository.findById(AdminAuthorizationUtil.getLoginAdminId())
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));
        return GetAdminUserProfileResponse.from(adminUser);
    }

    /**
     * 전체 관리자 정보 조회 메서드.
     *
     * Authorization 헤더에서 액세스 토큰을 추출하고, 해당 토큰의 소유자가 마스터 관리자일 경우
     * 시스템에 등록된 모든 관리자 정보를 조회하여 반환합니다.
     *
     * - 마스터 관리자가 아닐 경우 예외가 발생합니다.
     * - 토큰이 유효하지 않거나 관리자 정보가 존재하지 않을 경우 예외가 발생합니다.
     *
     * @return 전체 관리자 정보 리스트
     */
    @Transactional(readOnly = true)
    public List<AdminUserDetailResponse> getAllAdminUserDetail() {

        AdminUser adminUser = adminRepository.findById(AdminAuthorizationUtil.getLoginAdminId())
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));
        if (!AdminStatus.MASTER.equals(adminUser.getAdminStatus())) {
            throw new CustomException(AdminErrorStatus._ONLY_CAN_MASTER_ADMIN_USER);
        }

        return adminRepository.findAll().stream()
                .map(AdminUserDetailResponse::from)
                .toList();
    }

    /**
     * 관리자 권한 상태 수정 메서드.
     *
     * 요청자의 토큰에서 관리자 정보를 추출하고, 마스터 관리자 권한을 확인합니다.
     * 대상 관리자 ID를 통해 조회 후, 요청된 권한 상태로 업데이트합니다.
     *
     * - 마스터 관리자가 아닐 경우 예외가 발생합니다.
     * - 대상 관리자가 존재하지 않을 경우 예외가 발생합니다.
     *
     * @param request 수정할 관리자 ID와 변경할 권한 상태를 담은 요청 객체
     */
    @Transactional
    public void updateAdminUserStatus(UpdateAdminUserStatusRequest request) {

        AdminUser adminUser = adminRepository.findById(AdminAuthorizationUtil.getLoginAdminId())
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));
        if (!AdminStatus.MASTER.equals(adminUser.getAdminStatus())) {
            throw new CustomException(AdminErrorStatus._ONLY_CAN_MASTER_ADMIN_USER);
        }

        AdminUser targetAdminUser = adminRepository.findById(request.id())
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));
        targetAdminUser.updateAdminStatus(request.adminStatus());
    }

    /**
     * 관리자 계정 탈퇴 처리 메서드.
     *
     * 액세스 토큰을 기반으로 관리자 정보를 조회한 뒤 DB에서 삭제합니다.
     *
     */
    @Transactional
    public void withdrawAdminUser() {

        AdminUser adminUser = adminRepository.findById(AdminAuthorizationUtil.getLoginAdminId())
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));
        adminRepository.delete(adminUser);
    }

    /**
     * 대시보드 이벤트 목록 조회 메서드
     *
     * 어드민 권한 사용자가 전체 이벤트 목록을 페이지 단위로 조회할 수 있습니다.
     * 각 이벤트에 대해 스케줄 및 참여자 수를 일괄 조회한 뒤 DashboardEvent로 변환합니다.
     *
     * 정렬 기준이 participant_count인 경우 메모리 내 정렬 후 페이징 처리됩니다.
     * 그 외 기준은 DB 정렬 및 페이징 후 결과가 반환됩니다.
     *
     * @param pageable 페이지 정보 (페이지 번호, 크기 등 - 정렬은 직접 처리)
     * @param keyword 정렬 기준 필드명 (snake_case)
     * @param sorting 정렬 방향 ("asc", "desc")
     * @return DashboardEvent 리스트 및 페이지 정보 포함 응답 DTO
     */
    @Transactional(readOnly = true)
    public GetAllDashboardEventsResponse getAllDashboardEvents(Pageable pageable, String keyword, String sorting) {
        return getAllDashboardEvents(pageable, keyword, sorting, null, null, null);
    }

    @Transactional(readOnly = true)
    public GetAllDashboardEventsResponse getAllDashboardEvents(Pageable pageable, String keyword, String sorting,
                                                                String search, LocalDate startDate, LocalDate endDate) {
        return getAllDashboardEvents(pageable, keyword, sorting, search, startDate, endDate, null, null);
    }

    @Transactional(readOnly = true)
    public GetAllDashboardEventsResponse getAllDashboardEvents(Pageable pageable, String keyword, String sorting,
                                                                String search, LocalDate startDate, LocalDate endDate,
                                                                Integer hour, Integer dayOfWeek) {
        adminRepository.findById(AdminAuthorizationUtil.getLoginAdminId())
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

        // 검색/필터 적용된 이벤트 조회
        List<Event> pagedEvents = eventRepository.findAllWithFilters(pageable, keyword, sorting, search, startDateTime, endDateTime, hour, dayOfWeek);
        long totalElements = eventRepository.countWithFilters(search, startDateTime, endDateTime, hour, dayOfWeek);
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());

        List<Long> eventIds = pagedEvents.stream().map(Event::getId).toList();

        Map<Long, List<Schedule>> scheduleMap = scheduleRepository.findAllByEventIdIn(eventIds).stream()
                .collect(Collectors.groupingBy(s -> s.getEvent().getId()));

        List<Object[]> participantData = statisticsRepository.countParticipantsByEventIds(eventIds);
        Map<Long, Long> participantCountMap = participantData.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));

        // 생성자 정보 조회 (CREATOR 또는 CREATOR_AND_PARTICIPANT)
        Map<Long, String> creatorNicknameMap = eventParticipationRepository.findAllByEventIdIn(eventIds).stream()
                .filter(ep -> ep.getParticipationRole() == ParticipationRole.CREATOR || ep.getParticipationRole() == ParticipationRole.CREATOR_AND_PARTICIPANT)
                .filter(ep -> ep.getUser() != null)
                .collect(Collectors.toMap(
                        ep -> ep.getEvent().getId(),
                        ep -> ep.getUser().getNickname() != null ? ep.getUser().getNickname() : ep.getUser().getName(),
                        (existing, replacement) -> existing // 중복 시 첫 번째 값 유지
                ));

        List<DashboardEvent> dashboardEvents = pagedEvents.stream()
                .map(event -> {
                    List<Schedule> schedules = scheduleMap.getOrDefault(event.getId(), List.of());
                    int participantCount = participantCountMap.getOrDefault(event.getId(), 0L).intValue();
                    String creatorNickname = creatorNicknameMap.getOrDefault(event.getId(), "-");
                    return DashboardEvent.of(event, schedules, participantCount, creatorNickname);
                }).toList();

        PageInfo pageInfo = PageInfo.of(pageable.getPageNumber() + 1, pageable.getPageSize(), (int) totalElements, totalPages);
        return GetAllDashboardEventsResponse.of(dashboardEvents, pageInfo);
    }

    /**
     * 대시보드 사용자 목록 조회 메서드
     *
     * 어드민 권한 사용자가 전체 사용자 정보를 페이지 단위로 조회할 수 있습니다.
     * 사용자 목록은 정렬 기준(keyword)과 정렬 방향(sorting)에 따라 정렬되며,
     * 각 사용자 데이터는 참여 이벤트 수를 포함한 DashboardUser DTO로 변환됩니다.
     *
     * @param pageable 페이지 정보 (페이지 번호, 크기 등)
     * @param keyword 정렬 기준 필드 (예: name, email, created_date 등)
     * @param sorting 정렬 방향 ("asc" 또는 "desc")
     * @return DashboardUser 리스트 및 페이지 정보 포함 응답 DTO
     */
    @Transactional(readOnly = true)
    public GetAllDashboardUsersResponse getAllDashboardUsers(Pageable pageable, String keyword, String sorting) {
        return getAllDashboardUsers(pageable, keyword, sorting, null, null, null);
    }

    @Transactional(readOnly = true)
    public GetAllDashboardUsersResponse getAllDashboardUsers(Pageable pageable, String keyword, String sorting,
                                                              String search, LocalDate startDate, LocalDate endDate) {
        adminRepository.findById(AdminAuthorizationUtil.getLoginAdminId())
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

        // 검색/필터 적용된 유저 조회
        List<User> users = userRepository.findAllWithFilters(pageable, keyword, sorting, search, startDateTime, endDateTime);
        long totalElements = userRepository.countWithFilters(search, startDateTime, endDateTime);
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());

        // 배치 쿼리로 유저별 참여 수 한 번에 조회 (N+1 해결)
        List<Long> userIds = users.stream().map(User::getId).toList();
        Map<Long, Long> participationCountMap = statisticsRepository.countParticipationsByUserIds(userIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));

        List<DashboardUser> dashboardUsers = users.stream()
                .map(user -> {
                    int participantCount = participationCountMap.getOrDefault(user.getId(), 0L).intValue();
                    return DashboardUser.from(user, participantCount);
                }).toList();

        PageInfo pageInfo = PageInfo.of(
                pageable.getPageNumber() + 1,
                pageable.getPageSize(),
                (int) totalElements,
                totalPages
        );

        return GetAllDashboardUsersResponse.of(dashboardUsers, pageInfo);
    }
}
