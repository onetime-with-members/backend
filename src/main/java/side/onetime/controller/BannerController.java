package side.onetime.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import side.onetime.dto.banner.request.RegisterBannerRequest;
import side.onetime.dto.banner.request.RegisterBarBannerRequest;
import side.onetime.dto.banner.request.UpdateBannerRequest;
import side.onetime.dto.banner.request.UpdateBarBannerRequest;
import side.onetime.dto.banner.response.*;
import side.onetime.global.common.ApiResponse;
import side.onetime.global.common.status.SuccessStatus;
import side.onetime.service.BannerService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    /**
     * 배너 등록 API.
     *
     * 요청으로 전달된 정보를 바탕으로 새로운 배너를 등록합니다.
     * 기본적으로 비활성화 상태이며 삭제되지 않은 상태로 생성됩니다.
     *
     * @param authorizationHeader 액세스 토큰
     * @param request 배너 등록 요청 정보
     * @param imageFile 배너 등록 이미지 객체
     * @return 성공 응답 메시지
     */
    @PostMapping(value = "/banners/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SuccessStatus>> registerBanner(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestPart(value = "request") RegisterBannerRequest request,
            @RequestPart(value = "image_file") MultipartFile imageFile) {
        bannerService.registerBanner(authorizationHeader, request, imageFile);
        return ApiResponse.onSuccess(SuccessStatus._REGISTER_BANNER);
    }

    /**
     * 띠배너 등록 API.
     *
     * 요청으로 전달된 정보를 바탕으로 새로운 띠배너를 등록합니다.
     * 기본적으로 비활성화 상태이며 삭제되지 않은 상태로 생성됩니다.
     *
     * @param authorizationHeader 액세스 토큰
     * @param request 띠배너 등록 요청 정보
     * @return 성공 응답 메시지
     */
    @PostMapping("/bar-banners/register")
    public ResponseEntity<ApiResponse<SuccessStatus>> registerBarBanner(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody RegisterBarBannerRequest request) {
        bannerService.registerBarBanner(authorizationHeader, request);
        return ApiResponse.onSuccess(SuccessStatus._REGISTER_BAR_BANNER);
    }

    /**
     * 배너 단건 조회 API.
     *
     * 삭제되지 않은 배너 중, ID에 해당하는 배너를 조회합니다.
     *
     * @param authorizationHeader 액세스 토큰
     * @param id 조회할 배너 ID
     * @return 배너 응답 객체
     */
    @GetMapping("/banners/{id}")
    public ResponseEntity<ApiResponse<GetBannerResponse>> getBanner(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id) {
        GetBannerResponse response = bannerService.getBanner(authorizationHeader, id);
        return ApiResponse.onSuccess(SuccessStatus._GET_BANNER, response);
    }

    /**
     * 띠배너 단건 조회 API.
     *
     * 삭제되지 않은 띠배너 중, ID에 해당하는 띠배너를 조회합니다.
     *
     * @param authorizationHeader 액세스 토큰
     * @param id 조회할 띠배너 ID
     * @return 띠배너 응답 객체
     */
    @GetMapping("/bar-banners/{id}")
    public ResponseEntity<ApiResponse<GetBarBannerResponse>> getBarBanner(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id) {
        GetBarBannerResponse response = bannerService.getBarBanner(authorizationHeader, id);
        return ApiResponse.onSuccess(SuccessStatus._GET_BAR_BANNER, response);
    }

    /**
     * 배너 전체 조회 API.
     *
     * 삭제되지 않은 모든 배너를 조회합니다.
     *
     * @param authorizationHeader 액세스 토큰
     * @return 배너 응답 객체 리스트
     */
    @GetMapping("/banners/all")
    public ResponseEntity<ApiResponse<GetAllBannersResponse>> getAllBanners(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) int page
    ) {
        Pageable pageable = PageRequest.of(page - 1, 20);
        GetAllBannersResponse response = bannerService.getAllBanners(authorizationHeader, pageable);
        return ApiResponse.onSuccess(SuccessStatus._GET_ALL_BANNERS, response);
    }

    /**
     * 띠배너 전체 조회 API.
     *
     * 삭제되지 않은 모든 띠배너를 조회합니다.
     *
     * @param authorizationHeader 액세스 토큰
     * @return 띠배너 응답 객체 리스트
     */
    @GetMapping("/bar-banners/all")
    public ResponseEntity<ApiResponse<GetAllBarBannersResponse>> getAllBarBanners(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(value = "page", defaultValue = "1") @Min(1) int page
    ) {
        Pageable pageable = PageRequest.of(page - 1, 20);
        GetAllBarBannersResponse response = bannerService.getAllBarBanners(authorizationHeader, pageable);
        return ApiResponse.onSuccess(SuccessStatus._GET_ALL_BAR_BANNERS, response);
    }

    /**
     * 현재 활성화된 배너 전체 조회 API.
     *
     * @return 활성화된 배너 응답 객체 리스트
     */
    @GetMapping("/banners/activated/all")
    public ResponseEntity<ApiResponse<GetAllActivatedBannersResponse>> getAllActivatedBanners() {
        GetAllActivatedBannersResponse response = bannerService.getAllActivatedBanners();
        return ApiResponse.onSuccess(SuccessStatus._GET_ALL_ACTIVATED_BANNERS, response);
    }

    /**
     * 현재 활성화된 띠배너 전체 조회 API.
     *
     * @return 활성화된 띠배너 응답 객체 리스트
     */
    @GetMapping("/bar-banners/activated/all")
    public ResponseEntity<ApiResponse<GetAllActivatedBarBannersResponse>> getAllActivatedBarBanners() {
        GetAllActivatedBarBannersResponse response = bannerService.getAllActivatedBarBanners();
        return ApiResponse.onSuccess(SuccessStatus._GET_ALL_ACTIVATED_BAR_BANNERS, response);
    }

    /**
     * 배너 수정 API.
     *
     * 일부 필드만 수정이 가능한 PATCH 방식의 API입니다.
     * 전달받은 요청에서 null이 아닌 필드만 수정되며,
     * 삭제된 배너는 수정할 수 없습니다.
     *
     * @param authorizationHeader 액세스 토큰
     * @param id 수정할 배너 ID
     * @param request 수정 요청 DTO
     * @param imageFile 배너 수정 이미지 객체
     * @return 성공 응답 메시지
     */
    @PatchMapping(value = "/banners/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SuccessStatus>> updateBanner(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id,
            @Valid @RequestPart(value = "request") UpdateBannerRequest request,
            @RequestPart(value = "image_file", required = false) MultipartFile imageFile
    ) {
        bannerService.updateBanner(authorizationHeader, id, request, imageFile);
        return ApiResponse.onSuccess(SuccessStatus._UPDATE_BANNER);
    }

    /**
     * 띠배너 수정 API.
     *
     * 일부 필드만 수정이 가능한 PATCH 방식의 API입니다.
     * 전달받은 요청에서 null이 아닌 필드만 수정되며,
     * 삭제된 띠배너는 수정할 수 없습니다.
     *
     * @param authorizationHeader 액세스 토큰
     * @param id 수정할 띠배너 ID
     * @param request 수정 요청 DTO
     * @return 성공 응답 메시지
     */
    @PatchMapping("/bar-banners/{id}")
    public ResponseEntity<ApiResponse<SuccessStatus>> updateBarBanner(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id,
            @Valid @RequestBody UpdateBarBannerRequest request
    ) {
        bannerService.updateBarBanner(authorizationHeader, id, request);
        return ApiResponse.onSuccess(SuccessStatus._UPDATE_BAR_BANNER);
    }

    /**
     * 배너 삭제 API.
     *
     * 배너를 DB에서 실제로 삭제하지 않고, isDeleted 플래그만 true로 변경합니다.
     * 해당 배너는 이후 조회되지 않으며 비활성화 상태로 간주됩니다.
     *
     * @param authorizationHeader 액세스 토큰
     * @param id 삭제할 배너 ID
     * @return 성공 응답 메시지
     */
    @DeleteMapping("/banners/{id}")
    public ResponseEntity<ApiResponse<SuccessStatus>> deleteBanner(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id
    ) {
        bannerService.deleteBanner(authorizationHeader, id);
        return ApiResponse.onSuccess(SuccessStatus._DELETE_BANNER);
    }

    /**
     * 띠배너 삭제 API.
     *
     * 띠배너를 DB에서 실제로 삭제하지 않고, isDeleted 플래그만 true로 변경합니다.
     * 해당 띠배너는 이후 조회되지 않으며 비활성화 상태로 간주됩니다.
     *
     * @param authorizationHeader 액세스 토큰
     * @param id 삭제할 띠배너 ID
     * @return 성공 응답 메시지
     */
    @DeleteMapping("/bar-banners/{id}")
    public ResponseEntity<ApiResponse<SuccessStatus>> deleteBarBanner(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id
    ) {
        bannerService.deleteBarBanner(authorizationHeader, id);
        return ApiResponse.onSuccess(SuccessStatus._DELETE_BAR_BANNER);
    }

    /**
     * 배너 클릭 수 증가 API.
     *
     * 요청 ID에 해당하는 배너의 클릭 수가 1 증가합니다.
     *
     * @param id 클릭한 배너 ID
     * @return 성공 응답 메시지
     */
    @PatchMapping("/banners/{id}/clicks")
    public ResponseEntity<ApiResponse<SuccessStatus>> increaseBannerClickCount(@PathVariable Long id) {
        bannerService.increaseBannerClickCount(id);
        return ApiResponse.onSuccess(SuccessStatus._INCREASE_BANNER_CLICK_COUNT);
    }
}
