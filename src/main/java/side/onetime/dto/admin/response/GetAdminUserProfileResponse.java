package side.onetime.dto.admin.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import side.onetime.domain.AdminUser;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GetAdminUserProfileResponse(
        String name,
        String email
) {
    public static GetAdminUserProfileResponse from(AdminUser adminUser) {
        return new GetAdminUserProfileResponse(adminUser.getName(), adminUser.getEmail());
    }
}
