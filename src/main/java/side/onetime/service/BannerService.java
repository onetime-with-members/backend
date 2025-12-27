package side.onetime.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import side.onetime.dto.admin.response.GetAllActivatedBannersResponse;
import side.onetime.dto.admin.response.GetAllActivatedBarBannersResponse;
import side.onetime.dto.admin.response.GetBannerResponse;
import side.onetime.dto.admin.response.GetBarBannerResponse;
import side.onetime.repository.BannerRepository;
import side.onetime.repository.BarBannerRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;
    private final BarBannerRepository barBannerRepository;

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
}
