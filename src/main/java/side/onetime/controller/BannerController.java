package side.onetime.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import side.onetime.dto.admin.response.GetActivatedBannerResponse;
import side.onetime.global.common.ApiResponse;
import side.onetime.global.common.status.SuccessStatus;
import side.onetime.service.AdminService;

@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
public class BannerController {

    private final AdminService adminService;

    /**
     * 현재 활성화된 띠배너 조회 API.
     *
     * @return 활성화된 배너 정보
     */
    @GetMapping("/activated")
    public ResponseEntity<ApiResponse<GetActivatedBannerResponse>> getActivatedBanner() {
        GetActivatedBannerResponse response = adminService.getActivatedBanner();
        return ApiResponse.onSuccess(SuccessStatus._GET_ACTIVATED_BANNER, response);
    }
}
