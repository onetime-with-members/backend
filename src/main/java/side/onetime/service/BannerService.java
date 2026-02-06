package side.onetime.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import side.onetime.domain.Banner;
import side.onetime.domain.BannerStaging;
import side.onetime.dto.admin.response.PageInfo;
import side.onetime.dto.banner.request.ExportBannerRequest;
import side.onetime.dto.banner.request.RegisterBannerRequest;
import side.onetime.dto.banner.request.UpdateBannerRequest;
import side.onetime.dto.banner.response.GetAllActivatedBannersResponse;
import side.onetime.dto.banner.response.GetAllBannersResponse;
import side.onetime.dto.banner.response.GetBannerResponse;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.AdminErrorStatus;
import side.onetime.global.config.sync.BannerSyncProperties;
import side.onetime.repository.AdminRepository;
import side.onetime.repository.BannerRepository;
import side.onetime.repository.BannerStagingRepository;
import side.onetime.util.AdminAuthorizationUtil;
import side.onetime.util.S3Util;

@Slf4j
@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;
    private final AdminRepository adminRepository;
    private final BannerStagingRepository bannerStagingRepository;
    private final RestClient bannerClient;
    private final S3Util s3Util;
	private final BannerSyncProperties bannerSyncProperties;

    /**
     * 배너 등록 메서드.
     *
     * 요청 정보를 바탕으로 배너를 등록합니다.
     * 기본적으로 비활성화 및 삭제되지 않은 상태로 저장됩니다.
     *
     * @param request 배너 등록 요청 객체
     * @param imageFile 배너 등록 이미지 객체
     */
    @Transactional
    public void registerBanner(RegisterBannerRequest request, MultipartFile imageFile) {
        adminRepository.findById(AdminAuthorizationUtil.getLoginAdminId())
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));
        Banner newBanner = bannerRepository.save(request.toEntity());

        String imageUrl = uploadBannerImage(newBanner.getId(), imageFile);
        newBanner.updateImageUrl(imageUrl);
    }

    /**
     * 단일 배너 조회 메서드.
     *
     * 삭제되지 않은 상태의 배너를 ID 기준으로 조회합니다.
     * 해당 배너가 존재하지 않을 경우 예외가 발생합니다.
     *
     * @param id 조회할 배너 ID
     * @return 배너 응답 객체
     */
    @Transactional(readOnly = true)
    public GetBannerResponse getBanner(Long id) {
        adminRepository.findById(AdminAuthorizationUtil.getLoginAdminId())
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));
        Banner banner = bannerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_BANNER));
        return GetBannerResponse.from(banner);
    }

    /**
     * 전체 배너 조회 메서드.
     *
     * 삭제되지 않은 모든 배너를 조회하여 응답 객체로 반환합니다.
     *
     * @return 배너 응답 객체 리스트
     */
    @Transactional(readOnly = true)
    public GetAllBannersResponse getAllBanners(Pageable pageable) {
        adminRepository.findById(AdminAuthorizationUtil.getLoginAdminId())
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));

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
     * 활성화된 배너 전체 조회 메서드.
     *
     * 현재 활성화 상태이며 삭제되지 않은 배너를 전체 조회합니다.
     * - 없을 경우 빈 리스트를 반환합니다.
     *
     * @return 활성화된 배너 응답 객체 리스트 또는 빈 리스트
     */
    @Transactional(readOnly = true)
    public GetAllActivatedBannersResponse getAllActivatedBanners() {
        List<GetBannerResponse> banners = bannerRepository.findAllByIsActivatedTrueAndIsDeletedFalse().stream()
                .map(GetBannerResponse::from)
                .toList();
        return GetAllActivatedBannersResponse.from(banners);
    }

    /**
     * 배너 수정 메서드.
     *
     * 삭제되지 않은 배너를 ID 기준으로 조회합니다.
     * 요청 객체에서 null이 아닌 필드만 선택적으로 수정합니다.
     *
     * @param id 수정할 배너 ID
     * @param request 수정 요청 객체
     * @param imageFile 배너 수정 이미지 객체
     */
    @Transactional
    public void updateBanner(Long id, UpdateBannerRequest request, MultipartFile imageFile) {
        adminRepository.findById(AdminAuthorizationUtil.getLoginAdminId())
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));
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
     * 배너 삭제 메서드 (논리 삭제).
     *
     * 삭제되지 않은 배너를 ID 기준으로 조회합니다.
     * 해당 배너의 삭제 상태를 true로 변경하고 S3에 저장된 이미지를 삭제합니다.
     *
     * @param id 삭제할 배너 ID
     */
    @Transactional
    public void deleteBanner(Long id) {
        adminRepository.findById(AdminAuthorizationUtil.getLoginAdminId())
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));
        Banner banner = bannerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_BANNER));
        banner.markAsDeleted();

        String imageUrl = banner.getImageUrl();
        deleteExistingBannerImage(imageUrl);
    }

    /**
     * 배너 클릭 수 증가 메서드.
     *
     * ID에 해당하는 배너의 클릭 수가 1 증가합니다.
     *
     * @param id 클릭한 배너 ID
     */
    @Transactional
    public void increaseBannerClickCount(Long id) {
        bannerRepository.increaseClickCount(id);
    }

    /**
     * 배너 내보내기 메서드.
     *
     * 삭제되지 않은 모든 배너를 조회하여 요청 객체(DTO)로 변환합니다.
     * 운영 서버의 스테이징 저장 API(/api/v1/banners/staging)를 호출하여 데이터를 전달합니다.
     * 전달된 데이터는 운영 서버의 스테이징 영역에 보관됩니다.
     */
    @Transactional
    public void exportBanners() {
        adminRepository.findById(AdminAuthorizationUtil.getLoginAdminId())
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));
		
		if (!bannerSyncProperties.canExport()) {
			throw new CustomException(AdminErrorStatus._SYNC_DISABLED_ENVIRONMENT);
		}

        List<Banner> banners = bannerRepository.findAllByIsDeletedFalse();
        List<ExportBannerRequest> exportBannerRequests = banners.stream()
                .map(ExportBannerRequest::from)
                .toList();
        
        try {
            bannerClient.post()
                    .uri( "/api/v1/banners/staging")
                    .header("X-API-KEY", bannerSyncProperties.apiKey())
                    .body(exportBannerRequests)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("❌ 배너 내보내기 중 서버 통신 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(AdminErrorStatus._FAILED_EXPORT_TRANSMISSION);
        }
    }

    /**
     * 배너 불러오기 메서드.
     *
     * 1. 스테이징 테이블에 없는 기존 배너들을 조회하여 삭제합니다. (삭제 대상: 운영에서 직접 추가, 테스트 서버에서 삭제된 배너)
     * 2. 스테이징 데이터를 운영 테이블과 비교하여 반영합니다.
     *    - 이미 존재하는 배너(bannerStagingId 기준): 기존 클릭 수 및 활성 상태를 유지하며 정보를 업데이트합니다.
     *    - 새로운 배너: 신규 레코드로 생성하여 운영 테이블에 추가합니다.
     */
    @Transactional
    public void importBanners() {
        adminRepository.findById(AdminAuthorizationUtil.getLoginAdminId())
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));

        List<BannerStaging> bannerStagings = bannerStagingRepository.findAll();
        List<Long> bannerStagingIds = bannerStagings.stream()
                .map(BannerStaging::getBannerId)
                .toList();

        bannerRepository.findAllByIsDeletedFalse().stream()
                .filter(banner -> banner.getBannerStagingId() == null || !bannerStagingIds.contains(banner.getBannerStagingId()))
                .forEach(Banner::markAsDeleted);

        for (BannerStaging bannerStaging : bannerStagings) {
            bannerRepository.findByBannerStagingIdAndIsDeletedFalse(bannerStaging.getBannerId())
                    .ifPresentOrElse(
                            existingBanner -> {
                                existingBanner.updateOrganization(bannerStaging.getOrganization());
                                existingBanner.updateTitle(bannerStaging.getTitle());
                                existingBanner.updateSubTitle(bannerStaging.getSubTitle());
                                existingBanner.updateButtonText(bannerStaging.getButtonText());
                                existingBanner.updateColorCode(bannerStaging.getColorCode());
                                existingBanner.updateImageUrl(bannerStaging.getImageUrl());
                                existingBanner.updateLinkUrl(bannerStaging.getLinkUrl());
                            },
                            () -> bannerRepository.save(
                                    Banner.builder()
                                            .bannerStagingId(bannerStaging.getBannerId())
                                            .organization(bannerStaging.getOrganization())
                                            .title(bannerStaging.getTitle())
                                            .subTitle(bannerStaging.getSubTitle())
                                            .buttonText(bannerStaging.getButtonText())
                                            .colorCode(bannerStaging.getColorCode())
                                            .imageUrl(bannerStaging.getImageUrl())
                                            .linkUrl(bannerStaging.getLinkUrl())
                                            .build()
                            )
                    );
        }
    }

    /**
     * 배너 스테이징 저장 메서드.
     *
     * 기존에 저장되어 있던 모든 스테이징 데이터를 일괄 삭제 후, 새로운 데이터로 교체하여 최신화합니다.
     *
     * @param requestApiKey API 인증키
     * @param requests 테스트 서버로부터 전송된 배너 리스트
     */
    @Transactional
    public void saveBannerStaging(String requestApiKey, List<ExportBannerRequest> requests) {
		
		if (!bannerSyncProperties.canReceive()) {
			throw new CustomException(AdminErrorStatus._SYNC_DISABLED_ENVIRONMENT);
		}
        if (!bannerSyncProperties.apiKey().equals(requestApiKey)) {
            throw new CustomException(AdminErrorStatus._INVALID_API_KEY);
        }

        bannerStagingRepository.deleteAllInBatch();

        List<BannerStaging> bannerStagings = requests.stream()
                .map(ExportBannerRequest::toEntity)
                .toList();

        bannerStagingRepository.saveAll(bannerStagings);
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
