package side.onetime.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import side.onetime.domain.Banner;
import side.onetime.domain.BarBanner;
import side.onetime.dto.admin.response.PageInfo;
import side.onetime.dto.banner.request.RegisterBannerRequest;
import side.onetime.dto.banner.request.RegisterBarBannerRequest;
import side.onetime.dto.banner.request.UpdateBannerRequest;
import side.onetime.dto.banner.request.UpdateBarBannerRequest;
import side.onetime.dto.banner.response.*;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.AdminErrorStatus;
import side.onetime.repository.BannerRepository;
import side.onetime.repository.BarBannerRepository;
import side.onetime.util.JwtUtil;
import side.onetime.util.S3Util;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;
    private final BarBannerRepository barBannerRepository;
    private final JwtUtil jwtUtil;
    private final S3Util s3Util;

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
