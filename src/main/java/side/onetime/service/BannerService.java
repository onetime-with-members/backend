package side.onetime.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import side.onetime.repository.BannerRepository;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;

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
