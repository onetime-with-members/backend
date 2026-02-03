package side.onetime.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import side.onetime.dto.barbanner.request.ExportBarBannerRequest;
import side.onetime.dto.barbanner.request.RegisterBarBannerRequest;
import side.onetime.dto.barbanner.request.UpdateBarBannerRequest;
import side.onetime.dto.barbanner.response.GetAllActivatedBarBannersResponse;
import side.onetime.dto.barbanner.response.GetAllBarBannersResponse;
import side.onetime.dto.barbanner.response.GetBarBannerResponse;
import side.onetime.global.common.ApiResponse;
import side.onetime.global.common.status.SuccessStatus;
import side.onetime.service.BarBannerService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bar-banners")
@RequiredArgsConstructor
@Validated
public class BarBannerController {

    private final BarBannerService barBannerService;

    /**
     * 띠배너 등록 API.
     *
     * 요청으로 전달된 정보를 바탕으로 새로운 띠배너를 등록합니다.
     * 기본적으로 비활성화 상태이며 삭제되지 않은 상태로 생성됩니다.
     *
     * @param request 띠배너 등록 요청 정보
     * @return 성공 응답 메시지
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<SuccessStatus>> registerBarBanner(
            @Valid @RequestBody RegisterBarBannerRequest request
    ) {
        barBannerService.registerBarBanner(request);
        return ApiResponse.onSuccess(SuccessStatus._REGISTER_BAR_BANNER);
    }

    /**
     * 띠배너 단건 조회 API.
     *
     * 삭제되지 않은 띠배너 중, ID에 해당하는 띠배너를 조회합니다.
     *
     * @param id 조회할 띠배너 ID
     * @return 띠배너 응답 객체
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GetBarBannerResponse>> getBarBanner(@PathVariable Long id) {
        GetBarBannerResponse response = barBannerService.getBarBanner(id);
        return ApiResponse.onSuccess(SuccessStatus._GET_BAR_BANNER, response);
    }

    /**
     * 띠배너 전체 조회 API.
     *
     * 삭제되지 않은 모든 띠배너를 조회합니다.
     *
     * @return 띠배너 응답 객체 리스트
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<GetAllBarBannersResponse>> getAllBarBanners(
            @RequestParam(value = "page", defaultValue = "1") @Min(1) int page
    ) {
        Pageable pageable = PageRequest.of(page - 1, 20);
        GetAllBarBannersResponse response = barBannerService.getAllBarBanners(pageable);
        return ApiResponse.onSuccess(SuccessStatus._GET_ALL_BAR_BANNERS, response);
    }

    /**
     * 현재 활성화된 띠배너 전체 조회 API.
     *
     * @return 활성화된 띠배너 응답 객체 리스트
     */
    @GetMapping("/activated/all")
    public ResponseEntity<ApiResponse<GetAllActivatedBarBannersResponse>> getAllActivatedBarBanners() {
        GetAllActivatedBarBannersResponse response = barBannerService.getAllActivatedBarBanners();
        return ApiResponse.onSuccess(SuccessStatus._GET_ALL_ACTIVATED_BAR_BANNERS, response);
    }

    /**
     * 띠배너 수정 API.
     *
     * 일부 필드만 수정이 가능한 PATCH 방식의 API입니다.
     * 전달받은 요청에서 null이 아닌 필드만 수정되며,
     * 삭제된 띠배너는 수정할 수 없습니다.
     *
     * @param id 수정할 띠배너 ID
     * @param request 수정 요청 DTO
     * @return 성공 응답 메시지
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<SuccessStatus>> updateBarBanner(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBarBannerRequest request
    ) {
        barBannerService.updateBarBanner(id, request);
        return ApiResponse.onSuccess(SuccessStatus._UPDATE_BAR_BANNER);
    }

    /**
     * 띠배너 삭제 API.
     *
     * 띠배너를 DB에서 실제로 삭제하지 않고, isDeleted 플래그만 true로 변경합니다.
     * 해당 띠배너는 이후 조회되지 않으며 비활성화 상태로 간주됩니다.
     *
     * @param id 삭제할 띠배너 ID
     * @return 성공 응답 메시지
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<SuccessStatus>> deleteBarBanner(@PathVariable Long id) {
        barBannerService.deleteBarBanner(id);
        return ApiResponse.onSuccess(SuccessStatus._DELETE_BAR_BANNER);
    }

    /**
     * 띠배너 내보내기 API.
     *
     * 테스트 서버 전용 API로, 삭제되지 않은 모든 띠배너 데이터를 운영 서버의 스테이징 영역으로 전송합니다.
     *
     * @return 성공 응답 메시지
     */
    @PostMapping("/export")
    public ResponseEntity<ApiResponse<SuccessStatus>> exportBarBanners() {
        barBannerService.exportBarBanners();
        return ApiResponse.onSuccess(SuccessStatus._EXPORT_BAR_BANNERS);
    }

    /**
     * 띠배너 불러오기 API.
     *
     * 운영 서버 전용 API로, 스테이징 영역에 저장된 데이터를 실제 운영 환경의 띠배너 테이블에 동기화합니다.
     *
     * @return 성공 응답 메시지
     */
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<SuccessStatus>> importBarBanners() {
        barBannerService.importBarBanners();
        return ApiResponse.onSuccess(SuccessStatus._IMPORT_BAR_BANNERS);
    }

    /**
     * 띠배너 스테이징 저장 API.
     *
     * 테스트 서버로부터 전송받은 띠배너 리스트를 스테이징 테이블에 저장합니다.
     * 기존 스테이징 데이터는 모두 삭제된 후 새로운 데이터로 교체됩니다.
     *
     * @param requests 테스트 서버에서 전송한 띠배너 리스트
     * @return 성공 응답 메시지
     */
    @PostMapping("/staging")
    public ResponseEntity<ApiResponse<SuccessStatus>> saveBarBannerStaging(
            @RequestHeader(name = "X-API-KEY") String apiKey,
            @RequestBody List<ExportBarBannerRequest> requests
    ) {
        barBannerService.saveBarBannerStaging(apiKey, requests);
        return ApiResponse.onSuccess(SuccessStatus._SAVE_BAR_BANNER_STAGING);
    }
}
