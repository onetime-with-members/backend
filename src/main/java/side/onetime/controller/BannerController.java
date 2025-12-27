package side.onetime.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import side.onetime.dto.admin.response.GetAllActivatedBannersResponse;
import side.onetime.dto.admin.response.GetAllActivatedBarBannersResponse;
import side.onetime.global.common.ApiResponse;
import side.onetime.global.common.status.SuccessStatus;
import side.onetime.service.BannerService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

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
