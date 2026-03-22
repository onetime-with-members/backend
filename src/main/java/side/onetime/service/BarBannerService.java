package side.onetime.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import side.onetime.domain.BarBanner;
import side.onetime.domain.BarBannerStaging;
import side.onetime.dto.admin.response.PageInfo;
import side.onetime.dto.barbanner.request.ExportBarBannerRequest;
import side.onetime.dto.barbanner.request.RegisterBarBannerRequest;
import side.onetime.dto.barbanner.request.UpdateBarBannerRequest;
import side.onetime.dto.barbanner.response.GetAllActivatedBarBannersResponse;
import side.onetime.dto.barbanner.response.GetAllBarBannersResponse;
import side.onetime.dto.barbanner.response.GetBarBannerResponse;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.AdminErrorStatus;
import side.onetime.global.config.sync.BannerSyncProperties;
import side.onetime.repository.AdminRepository;
import side.onetime.repository.BarBannerRepository;
import side.onetime.repository.BarBannerStagingRepository;
import side.onetime.util.AdminAuthorizationUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class BarBannerService {
	
    private final BarBannerRepository barBannerRepository;
    private final AdminRepository adminRepository;
    private final BarBannerStagingRepository barBannerStagingRepository;
    private final RestClient bannerClient;
	private final BannerSyncProperties bannerSyncProperties;

    /**
     * 띠배너 등록 메서드.
     *
     * 요청 정보를 바탕으로 띠배너를 등록합니다.
     * 기본적으로 비활성화 및 삭제되지 않은 상태로 저장됩니다.
     *
     * @param request 띠배너 등록 요청 객체
     */
    @Transactional
    public void registerBarBanner(RegisterBarBannerRequest request) {
        adminRepository.findById(AdminAuthorizationUtil.getLoginAdminId())
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));
        BarBanner newBarBanner = request.toEntity();
        barBannerRepository.save(newBarBanner);
    }

    /**
     * 단일 띠배너 조회 메서드.
     *
     * 삭제되지 않은 상태의 배너를 ID 기준으로 조회합니다.
     * 해당 띠배너가 존재하지 않을 경우 예외가 발생합니다.
     *
     * @param id 조회할 띠배너 ID
     * @return 띠배너 응답 객체
     */
    @Transactional(readOnly = true)
    public GetBarBannerResponse getBarBanner(Long id) {
        adminRepository.findById(AdminAuthorizationUtil.getLoginAdminId())
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));
        BarBanner barBanner = barBannerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_BAR_BANNER));
        return GetBarBannerResponse.from(barBanner);
    }

    /**
     * 전체 띠배너 조회 메서드.
     *
     * 삭제되지 않은 모든 띠배너를 조회하여 응답 객체로 반환합니다.
     *
     * @return 띠배너 응답 객체 리스트
     */
    @Transactional(readOnly = true)
    public GetAllBarBannersResponse getAllBarBanners(Pageable pageable) {
        adminRepository.findById(AdminAuthorizationUtil.getLoginAdminId())
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));

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
     * 활성화된 띠배너 전체 조회 메서드.
     *
     * 현재 활성화 상태이며 삭제되지 않은 띠배너를 전체 조회합니다.
     * - 없을 경우 빈 리스트를 반환합니다.
     *
     * @return 활성화된 띠배너 응답 객체 리스트 또는 빈 리스트
     */
    @Transactional(readOnly = true)
    public GetAllActivatedBarBannersResponse getAllActivatedBarBanners() {
        List<GetBarBannerResponse> barBanners = barBannerRepository.findAllByIsActivatedTrueAndIsDeletedFalse().stream()
                .map(GetBarBannerResponse::from)
                .toList();
        return GetAllActivatedBarBannersResponse.from(barBanners);
    }

    /**
     * 띠배너 수정 메서드.
     *
     * 삭제되지 않은 띠배너를 ID 기준으로 조회합니다.
     * 요청 객체에서 null이 아닌 필드만 선택적으로 수정합니다.
     *
     * @param id 수정할 띠배너 ID
     * @param request 수정 요청 객체
     */
    @Transactional
    public void updateBarBanner(Long id, UpdateBarBannerRequest request) {
        adminRepository.findById(AdminAuthorizationUtil.getLoginAdminId())
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));
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
     * 띠배너 삭제 메서드 (논리 삭제).
     *
     * 삭제되지 않은 띠배너를 ID 기준으로 조회합니다.
     * 해당 배너의 삭제 상태를 true로 변경합니다.
     *
     * @param id 삭제할 띠배너 ID
     */
    @Transactional
    public void deleteBarBanner(Long id) {
        adminRepository.findById(AdminAuthorizationUtil.getLoginAdminId())
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));
        BarBanner barBanner = barBannerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_BAR_BANNER));
        barBanner.markAsDeleted();
    }

    /**
     * 띠배너 내보내기 메서드.
     *
     * 삭제되지 않은 모든 띠배너를 조회하여 요청 객체(DTO)로 변환합니다.
     * 운영 서버의 스테이징 저장 API(/api/v1/bar-banners/staging)를 호출하여 데이터를 전달합니다.
     * 전달된 데이터는 운영 서버의 스테이징 영역에 보관됩니다.
     */
    @Transactional(readOnly = true)
    public void exportBarBanners() {
        adminRepository.findById(AdminAuthorizationUtil.getLoginAdminId())
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));
		
		if (!bannerSyncProperties.canExport()) {
			throw new CustomException(AdminErrorStatus._SYNC_DISABLED_ENVIRONMENT);
		}

        List<BarBanner> barBanners = barBannerRepository.findAllByIsDeletedFalse();
        List<ExportBarBannerRequest> exportBarBannerRequests = barBanners.stream()
                .map(ExportBarBannerRequest::from)
                .toList();
		
        try {
            bannerClient.post()
                    .uri( "/api/v1/bar-banners/staging")
                    .header("X-API-KEY", bannerSyncProperties.apiKey())
                    .body(exportBarBannerRequests)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("❌ 띠배너 내보내기 중 서버 통신 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(AdminErrorStatus._FAILED_EXPORT_TRANSMISSION);
        }
    }

    /**
     * 띠배너 불러오기 메서드.
     *
     * 1. 스테이징 테이블에 없는 기존 띠배너들을 조회하여 삭제합니다. (삭제 대상: 운영에서 직접 추가, 테스트 서버에서 삭제된 띠배너)
     * 2. 스테이징 데이터를 운영 테이블과 비교하여 반영합니다.
     *    - 이미 존재하는 띠배너(barBannerStagingId 기준): 활성 상태를 유지하며 정보를 업데이트합니다.
     *    - 새로운 띠배너: 신규 레코드로 생성하여 운영 테이블에 추가합니다.
     */
    @Transactional
    public void importBarBanners() {
        adminRepository.findById(AdminAuthorizationUtil.getLoginAdminId())
                .orElseThrow(() -> new CustomException(AdminErrorStatus._NOT_FOUND_ADMIN_USER));

        List<BarBannerStaging> barBannerStagings = barBannerStagingRepository.findAll();
        List<Long> barBannerStagingIds = barBannerStagings.stream()
                .map(BarBannerStaging::getBarBannerId)
                .toList();

        barBannerRepository.findAllByIsDeletedFalse().stream()
                .filter(barBanner -> barBanner.getBarBannerStagingId() == null || !barBannerStagingIds.contains(barBanner.getBarBannerStagingId()))
                .forEach(BarBanner::markAsDeleted);

        for (BarBannerStaging barBannerStaging : barBannerStagings) {
            barBannerRepository.findByBarBannerStagingIdAndIsDeletedFalse(barBannerStaging.getBarBannerId())
                    .ifPresentOrElse(
                            existingBarBanner -> {
                                existingBarBanner.updateContentKor(barBannerStaging.getContentKor());
                                existingBarBanner.updateContentEng(barBannerStaging.getContentEng());
                                existingBarBanner.updateBackgroundColorCode(barBannerStaging.getBackgroundColorCode());
                                existingBarBanner.updateTextColorCode(barBannerStaging.getTextColorCode());
                                existingBarBanner.updateLinkUrl(barBannerStaging.getLinkUrl());
                            },
                            () -> barBannerRepository.save(
                                    BarBanner.builder()
                                            .barBannerStagingId(barBannerStaging.getBarBannerId())
                                            .contentKor(barBannerStaging.getContentKor())
                                            .contentEng(barBannerStaging.getContentEng())
                                            .backgroundColorCode(barBannerStaging.getBackgroundColorCode())
                                            .textColorCode(barBannerStaging.getTextColorCode())
                                            .linkUrl(barBannerStaging.getLinkUrl())
                                            .build()
                            )
                    );
        }

        barBannerStagings.forEach(BarBannerStaging::markAsImported);
    }

    /**
     * 띠배너 스테이징 데이터 건수 조회 메서드.
     *
     * @return 스테이징 테이블에 저장된 띠배너 수
     */
    @Transactional(readOnly = true)
    public long getBarBannerStagingCount() {
        return barBannerStagingRepository.countByIsImportedFalse();
    }

    /**
     * 띠배너 스테이징 저장 메서드.
     *
     * 기존에 저장되어 있던 모든 스테이징 데이터를 일괄 삭제 후, 새로운 데이터로 교체하여 최신화합니다.
     *
     * @param requestApiKey API 인증키
     * @param requests 테스트 서버로부터 전송된 띠배너 리스트
     */
    @Transactional
    public void saveBarBannerStaging(String requestApiKey, List<ExportBarBannerRequest> requests) {
		
		if (!bannerSyncProperties.canReceive()) {
			throw new CustomException(AdminErrorStatus._SYNC_DISABLED_ENVIRONMENT);
		}
        if (!bannerSyncProperties.apiKey().equals(requestApiKey)) {
            throw new CustomException(AdminErrorStatus._INVALID_API_KEY);
        }

        barBannerStagingRepository.deleteAllInBatch();

        List<BarBannerStaging> barBannerStagings = requests.stream()
                .map(ExportBarBannerRequest::toEntity)
                .toList();

        barBannerStagingRepository.saveAll(barBannerStagings);
    }
}
