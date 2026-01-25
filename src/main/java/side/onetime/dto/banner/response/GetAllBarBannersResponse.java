package side.onetime.dto.banner.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import side.onetime.dto.admin.response.PageInfo;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GetAllBarBannersResponse(
        List<GetBarBannerResponse> barBanners,
        PageInfo pageInfo
) {
    public static GetAllBarBannersResponse of(List<GetBarBannerResponse> barBanners, PageInfo pageInfo) {
        return new GetAllBarBannersResponse(
                barBanners,
                pageInfo
        );
    }
}
