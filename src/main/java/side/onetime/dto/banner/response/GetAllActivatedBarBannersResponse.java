package side.onetime.dto.banner.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GetAllActivatedBarBannersResponse(
        List<GetBarBannerResponse> barBanners
) {
    public static GetAllActivatedBarBannersResponse from(List<GetBarBannerResponse> barBanners) {
        return new GetAllActivatedBarBannersResponse(barBanners);
    }
}
