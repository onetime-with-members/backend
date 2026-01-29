package side.onetime.dto.admin.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GetAllDashboardUsersResponse(
        List<DashboardUser> dashboardUsers,
        PageInfo pageInfo
) {
    public static GetAllDashboardUsersResponse of(List<DashboardUser> dashboardUsers, PageInfo pageInfo) {
        return new GetAllDashboardUsersResponse(
                dashboardUsers,
                pageInfo
        );
    }
}
