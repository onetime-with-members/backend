package side.onetime.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import side.onetime.domain.*;
import side.onetime.domain.enums.AdminStatus;
import side.onetime.domain.enums.EventStatus;
import side.onetime.dto.admin.request.*;
import side.onetime.dto.admin.response.*;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.AdminErrorStatus;
import side.onetime.repository.*;
import side.onetime.util.JwtUtil;
import side.onetime.util.S3Util;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final EventRepository eventRepository;
    private final EventParticipationRepository eventParticipationRepository;
    private final ScheduleRepository scheduleRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final BannerRepository bannerRepository;
    private final BarBannerRepository barBannerRepository;
    private final JwtUtil jwtUtil;
    private final S3Util s3Util;

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
        AdminUser newAdminUser = request.toEntity();
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
     */
    @Transactional(readOnly = true)
    public LoginAdminUserResponse loginAdminUser(LoginAdminUserRequest request) {

        AdminUser adminUser = adminRepository.findAdminUserByEmail(request.email())
                        .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));
        if (AdminStatus.PENDING_APPROVAL == adminUser.getAdminStatus()) {
            throw new CustomException(AdminErrorStatus._IS_NOT_APPROVED_ADMIN_USER);
        }
        if (!request.password().equals(adminUser.getPassword())) {
            throw new CustomException(AdminErrorStatus._IS_NOT_EQUAL_PASSWORD);
        }
        return LoginAdminUserResponse.of(jwtUtil.generateAccessToken(adminUser.getId(), "ADMIN"));
    }

    /**
     * 관리자 프로필 조회 메서드.
     *
     * Authorization 헤더에서 액세스 토큰을 추출하고, 토큰에 포함된 ID를 기반으로
     * 관리자 정보를 조회합니다.
     * - 토큰이 유효하지 않거나 관리자 정보가 존재하지 않을 경우 예외가 발생합니다.
     *
     * @param authorizationHeader Authorization 헤더에 포함된 액세스 토큰
     * @return 관리자 프로필 응답 객체
     */
    @Transactional(readOnly = true)
    public GetAdminUserProfileResponse getAdminUserProfile(String authorizationHeader) {

        AdminUser adminUser = jwtUtil.getAdminUserFromHeader(authorizationHeader);
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
     * @param authorizationHeader Authorization 헤더에 포함된 액세스 토큰
     * @return 전체 관리자 정보 리스트
     */
    @Transactional(readOnly = true)
    public List<AdminUserDetailResponse> getAllAdminUserDetail(String authorizationHeader) {

        AdminUser adminUser = jwtUtil.getAdminUserFromHeader(authorizationHeader);
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
     * @param authorizationHeader 요청자의 액세스 토큰
     * @param request 수정할 관리자 ID와 변경할 권한 상태를 담은 요청 객체
     */
    @Transactional
    public void updateAdminUserStatus(String authorizationHeader, UpdateAdminUserStatusRequest request) {

        AdminUser adminUser = jwtUtil.getAdminUserFromHeader(authorizationHeader);
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
     * @param authorizationHeader Authorization 헤더에 포함된 액세스 토큰
     */
    @Transactional
    public void withdrawAdminUser(String authorizationHeader) {

        AdminUser adminUser = jwtUtil.getAdminUserFromHeader(authorizationHeader);
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
     * @param authorizationHeader Authorization 헤더에서 추출한 토큰
     * @param pageable 페이지 정보 (페이지 번호, 크기 등 - 정렬은 직접 처리)
     * @param keyword 정렬 기준 필드명 (snake_case)
     * @param sorting 정렬 방향 ("asc", "desc")
     * @return DashboardEvent 리스트 및 페이지 정보 포함 응답 DTO
     */
    @Transactional(readOnly = true)
    public GetAllDashboardEventsResponse getAllDashboardEvents(String authorizationHeader, Pageable pageable, String keyword, String sorting) {
        jwtUtil.getAdminUserFromHeader(authorizationHeader);

        boolean isSortByParticipant = keyword.equals("participant_count");

        List<Event> allEvents = isSortByParticipant
                ? eventRepository.findAll()
                : eventRepository.findAllWithSort(pageable, keyword, sorting);

        int totalElements = (int) eventRepository.count();
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());

        List<Event> pagedEvents = isSortByParticipant
                ? allEvents.stream()
                .sorted(Comparator.comparingInt(event -> {
                    Long eventId = event.getId();
                    int userCount = (int) eventParticipationRepository.findAllByEventIdIn(List.of(eventId)).stream()
                            .filter(ep -> ep.getEventStatus() != EventStatus.CREATOR)
                            .count();
                    int memberCount = (int) memberRepository.findAllByEventIdIn(List.of(eventId)).stream().count();
                    return userCount + memberCount;
                }))
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList()
                : allEvents;

        List<Long> eventIds = pagedEvents.stream().map(Event::getId).toList();

        Map<Long, List<Schedule>> scheduleMap = scheduleRepository.findAllByEventIdIn(eventIds).stream()
                .collect(Collectors.groupingBy(s -> s.getEvent().getId()));

        Map<Long, List<EventParticipation>> epMap = eventParticipationRepository.findAllByEventIdIn(eventIds).stream()
                .filter(ep -> ep.getEventStatus() != EventStatus.CREATOR)
                .collect(Collectors.groupingBy(ep -> ep.getEvent().getId()));

        Map<Long, List<Member>> memberMap = memberRepository.findAllByEventIdIn(eventIds).stream()
                .collect(Collectors.groupingBy(m -> m.getEvent().getId()));

        List<DashboardEvent> dashboardEvents = pagedEvents.stream()
                .map(event -> {
                    List<Schedule> schedules = scheduleMap.getOrDefault(event.getId(), List.of());
                    int userCount = epMap.getOrDefault(event.getId(), List.of()).size();
                    int memberCount = memberMap.getOrDefault(event.getId(), List.of()).size();
                    return DashboardEvent.of(event, schedules, userCount + memberCount);
                }).toList();

        PageInfo pageInfo = PageInfo.of(pageable.getPageNumber() + 1, pageable.getPageSize(), totalElements, totalPages);
        return GetAllDashboardEventsResponse.of(dashboardEvents, pageInfo);
    }

    /**
     * 대시보드 사용자 목록 조회 메서드
     *
     * 어드민 권한 사용자가 전체 사용자 정보를 페이지 단위로 조회할 수 있습니다.
     * 사용자 목록은 정렬 기준(keyword)과 정렬 방향(sorting)에 따라 정렬되며,
     * 각 사용자 데이터는 참여 이벤트 수를 포함한 DashboardUser DTO로 변환됩니다.
     *
     * 전체 유저 수를 기준으로 totalElements와 totalPages를 계산하여 PageInfo에 포함합니다.
     *
     * @param authorizationHeader Authorization 헤더에서 추출한 토큰
     * @param pageable 페이지 정보 (페이지 번호, 크기 등)
     * @param keyword 정렬 기준 필드 (예: name, email, created_date 등)
     * @param sorting 정렬 방향 ("asc" 또는 "desc")
     * @return DashboardUser 리스트 및 페이지 정보 포함 응답 DTO
     */
    @Transactional(readOnly = true)
    public GetAllDashboardUsersResponse getAllDashboardUsers(String authorizationHeader, Pageable pageable, String keyword, String sorting) {
        jwtUtil.getAdminUserFromHeader(authorizationHeader);

        List<User> users = userRepository.findAllWithSort(pageable, keyword, sorting);

        List<DashboardUser> dashboardUsers = users.stream()
                .map(user -> {
                    int participantCount = eventParticipationRepository.findAllByUserWithEvent(user).size();
                    return DashboardUser.from(user, participantCount);
                }).toList();

        int totalElements = (int) userRepository.count();
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());

        PageInfo pageInfo = PageInfo.of(
                pageable.getPageNumber() + 1,
                pageable.getPageSize(),
                totalElements,
                totalPages
        );

        return GetAllDashboardUsersResponse.of(dashboardUsers, pageInfo);
    }

    /**
     * 배너 등록 메서드.
     *
     * 요청 정보를 바탕으로 배너를 등록합니다.
     * 기본적으로 비활성화 및 삭제되지 않은 상태로 저장됩니다.
     *
     * @param authorizationHeader 요청자의 액세스 토큰
     * @param request 배너 등록 요청 객체
     * @param imageFile 배너 등록 이미지 객체
     */
    @Transactional
    public void registerBanner(String authorizationHeader, RegisterBannerRequest request, MultipartFile imageFile) {
        jwtUtil.getAdminUserFromHeader(authorizationHeader);
        Banner newBanner = bannerRepository.save(request.toEntity());

        String imageUrl = uploadBannerImage(newBanner.getId(), imageFile);
        newBanner.updateImageUrl(imageUrl);
    }

    /**
     * 띠배너 등록 메서드.
     *
     * 요청 정보를 바탕으로 배너를 등록합니다.
     * 기본적으로 비활성화 및 삭제되지 않은 상태로 저장됩니다.
     *
     * @param authorizationHeader 요청자의 액세스 토큰
     * @param request 띠배너 등록 요청 객체
     */
    @Transactional
    public void registerBarBanner(String authorizationHeader, RegisterBarBannerRequest request) {
        jwtUtil.getAdminUserFromHeader(authorizationHeader);
        BarBanner newBarBanner = request.toEntity();
        barBannerRepository.save(newBarBanner);
    }

    /**
     * 단일 배너 조회 메서드.
     *
     * 삭제되지 않은 상태의 배너를 ID 기준으로 조회합니다.
     * 해당 배너가 존재하지 않을 경우 예외가 발생합니다.
     *
     * @param authorizationHeader 요청자의 액세스 토큰
     * @param id 조회할 배너 ID
     * @return 배너 응답 객체
     */
    @Transactional(readOnly = true)
    public GetBannerResponse getBanner(String authorizationHeader, Long id) {
        jwtUtil.getAdminUserFromHeader(authorizationHeader);
        Banner banner = bannerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_BANNER));
        return GetBannerResponse.from(banner);
    }

    /**
     * 단일 띠배너 조회 메서드.
     *
     * 삭제되지 않은 상태의 배너를 ID 기준으로 조회합니다.
     * 해당 띠배너가 존재하지 않을 경우 예외가 발생합니다.
     *
     * @param authorizationHeader 요청자의 액세스 토큰
     * @param id 조회할 띠배너 ID
     * @return 띠배너 응답 객체
     */
    @Transactional(readOnly = true)
    public GetBarBannerResponse getBarBanner(String authorizationHeader, Long id) {
        jwtUtil.getAdminUserFromHeader(authorizationHeader);
        BarBanner barBanner = barBannerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_BAR_BANNER));
        return GetBarBannerResponse.from(barBanner);
    }

    /**
     * 전체 배너 조회 메서드.
     *
     * 삭제되지 않은 모든 배너를 조회하여 응답 객체로 반환합니다.
     *
     * @param authorizationHeader 요청자의 액세스 토큰
     * @return 배너 응답 객체 리스트
     */
    @Transactional(readOnly = true)
    public GetAllBannersResponse getAllBanners(String authorizationHeader, Pageable pageable) {
        jwtUtil.getAdminUserFromHeader(authorizationHeader);

        List<GetBannerResponse> banners = bannerRepository.findAllByIsDeletedFalseOrderByCreatedDateDesc(pageable).stream()
                .map(GetBannerResponse::from)
                .toList();

        int totalElements = (int) bannerRepository.countByIsDeletedFalse();
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());

        PageInfo pageInfo = PageInfo.of(
                pageable.getPageNumber() + 1,
                pageable.getPageSize(),
                totalElements,
                totalPages
        );

        return GetAllBannersResponse.of(banners, pageInfo);
    }

    /**
     * 전체 띠배너 조회 메서드.
     *
     * 삭제되지 않은 모든 띠배너를 조회하여 응답 객체로 반환합니다.
     *
     * @param authorizationHeader 요청자의 액세스 토큰
     * @return 띠배너 응답 객체 리스트
     */
    @Transactional(readOnly = true)
    public GetAllBarBannersResponse getAllBarBanners(String authorizationHeader, Pageable pageable) {
        jwtUtil.getAdminUserFromHeader(authorizationHeader);

        List<GetBarBannerResponse> barBanners = barBannerRepository.findAllByIsDeletedFalseOrderByCreatedDateDesc(pageable).stream()
                .map(GetBarBannerResponse::from)
                .toList();

        int totalElements = (int) barBannerRepository.countByIsDeletedFalse();
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());

        PageInfo pageInfo = PageInfo.of(
                pageable.getPageNumber() + 1,
                pageable.getPageSize(),
                totalElements,
                totalPages
        );

        return GetAllBarBannersResponse.of(barBanners, pageInfo);
    }

    /**
     * 배너 수정 메서드.
     *
     * 삭제되지 않은 배너를 ID 기준으로 조회합니다.
     * 요청 객체에서 null이 아닌 필드만 선택적으로 수정합니다.
     *
     * @param authorizationHeader 요청자의 액세스 토큰
     * @param id 수정할 배너 ID
     * @param request 수정 요청 객체
     * @param imageFile 배너 수정 이미지 객체
     */
    @Transactional
    public void updateBanner(String authorizationHeader, Long id, UpdateBannerRequest request, MultipartFile imageFile) {
        jwtUtil.getAdminUserFromHeader(authorizationHeader);
        Banner banner = bannerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_BANNER));

        if (request.organization() != null) banner.updateOrganization(request.organization());
        if (request.title() != null) banner.updateTitle(request.title());
        if (request.subTitle() != null) banner.updateSubTitle(request.subTitle());
        if (request.buttonText() != null) banner.updateButtonText(request.buttonText());
        if (request.colorCode() != null) banner.updateColorCode(request.colorCode());
        if (request.linkUrl() != null) banner.updateLinkUrl(request.linkUrl());
        if (request.isActivated() != null) banner.updateIsActivated(request.isActivated());

        if (imageFile != null) {
            deleteExistingBannerImage(banner.getImageUrl());

            String newImageUrl = uploadBannerImage(banner.getId(), imageFile);
            banner.updateImageUrl(newImageUrl);
        }
    }

    /**
     * 띠배너 수정 메서드.
     *
     * 삭제되지 않은 띠배너를 ID 기준으로 조회합니다.
     * 요청 객체에서 null이 아닌 필드만 선택적으로 수정합니다.
     *
     * @param authorizationHeader 요청자의 액세스 토큰
     * @param id 수정할 띠배너 ID
     * @param request 수정 요청 객체
     */
    @Transactional
    public void updateBarBanner(String authorizationHeader, Long id, UpdateBarBannerRequest request) {
        jwtUtil.getAdminUserFromHeader(authorizationHeader);
        BarBanner barBanner = barBannerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_BAR_BANNER));

        if (request.contentKor() != null) barBanner.updateContentKor(request.contentKor());
        if (request.contentEng() != null) barBanner.updateContentEng(request.contentEng());
        if (request.backgroundColorCode() != null) barBanner.updateBackgroundColorCode(request.backgroundColorCode());
        if (request.textColorCode() != null) barBanner.updateTextColorCode(request.textColorCode());
        if (request.linkUrl() != null) barBanner.updateLinkUrl(request.linkUrl());
        if (request.isActivated() != null) barBanner.updateIsActivated(request.isActivated());
    }

    /**
     * 배너 삭제 메서드 (논리 삭제).
     *
     * 삭제되지 않은 배너를 ID 기준으로 조회합니다.
     * 해당 배너의 삭제 상태를 true로 변경하고 S3에 저장된 이미지를 삭제합니다.
     *
     * @param authorizationHeader 요청자의 액세스 토큰
     * @param id 삭제할 배너 ID
     */
    @Transactional
    public void deleteBanner(String authorizationHeader, Long id) {
        jwtUtil.getAdminUserFromHeader(authorizationHeader);
        Banner banner = bannerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_BANNER));
        banner.markAsDeleted();

        String imageUrl = banner.getImageUrl();
        deleteExistingBannerImage(imageUrl);
    }

    /**
     * 띠배너 삭제 메서드 (논리 삭제).
     *
     * 삭제되지 않은 띠배너를 ID 기준으로 조회합니다.
     * 해당 배너의 삭제 상태를 true로 변경합니다.
     *
     * @param authorizationHeader 요청자의 액세스 토큰
     * @param id 삭제할 띠배너 ID
     */
    @Transactional
    public void deleteBarBanner(String authorizationHeader, Long id) {
        jwtUtil.getAdminUserFromHeader(authorizationHeader);
        BarBanner barBanner = barBannerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_BAR_BANNER));
        barBanner.markAsDeleted();
    }

    /**
     * S3에 배너 이미지를 업로드하는 메서드.
     * 주어진 이벤트 ID를 기반으로 QR 코드를 생성한 후, S3에 업로드합니다.
     *
     * @param bannerId 업로드할 배너의 ID
     * @param imageFile 업로드할 이미지 파일
     * @return S3에 업로드된 이미지 Public URL
     * @throws CustomException S3 업로드 실패 시 발생
     */
    private String uploadBannerImage(Long bannerId, MultipartFile imageFile) {
        try {
            String imageFileName = s3Util.uploadImage("banner/" + bannerId, imageFile);
            return s3Util.getPublicUrl(imageFileName);
        } catch (Exception e) {
            throw new CustomException(AdminErrorStatus._FAILED_UPLOAD_BANNER_IMAGE);
        }
    }

    /**
     * 기존 배너 이미지를 S3에서 삭제하는 메서드.
     *
     * @param imageUrl 삭제할 이미지 파일
     */
    private void deleteExistingBannerImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                String imageFileKey = S3Util.extractKey(imageUrl);
                s3Util.deleteFile(imageFileKey);
            } catch (Exception e) {
                log.warn("❌ 배너 이미지 삭제 예외 발생 - 요청 IMAGE_URI: {}", imageUrl, e);
            }
        }
    }
}
