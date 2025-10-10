package side.onetime.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import side.onetime.global.common.ApiResponse;
import side.onetime.global.common.status.SuccessStatus;
import side.onetime.service.BannerService;

@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    /**
     * 배너 클릭 수 증가 API.
     *
     * 요청 ID에 해당하는 배너의 클릭 수가 1 증가합니다.
     *
     * @param id 클릭한 배너 ID
     * @return 성공 응답 메시지
     */
    @PatchMapping("/{id}/clicks")
    public ResponseEntity<ApiResponse<SuccessStatus>> increaseBannerClickCount(@PathVariable Long id) {
        bannerService.increaseBannerClickCount(id);
        return ApiResponse.onSuccess(SuccessStatus._INCREASE_BANNER_CLICK_COUNT);
    }
}
