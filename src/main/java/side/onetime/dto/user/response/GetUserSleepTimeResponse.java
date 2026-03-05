package side.onetime.dto.user.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import side.onetime.domain.User;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GetUserSleepTimeResponse(
        String sleepStartTime,
        String sleepEndTime
) {
    public static GetUserSleepTimeResponse from(User user) {
        return new GetUserSleepTimeResponse(
                user.getSleepStartTime(),
                user.getSleepEndTime()
        );
    }
}
