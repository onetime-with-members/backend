package side.onetime.dto.banner.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GetAllActivatedBannersResponse(
        List<GetBannerResponse> banners
) {
    public static GetAllActivatedBannersResponse from(List<GetBannerResponse> banners) {
        return new GetAllActivatedBannersResponse(banners);
    }
}
