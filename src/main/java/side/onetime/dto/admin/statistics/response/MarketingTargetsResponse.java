package side.onetime.dto.admin.statistics.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MarketingTargetsResponse(
        long marketingAgreedUsers,
        long dormantUsers,
        long noEventUsers,
        long oneTimeUsers,
        long vipUsers,
        long zeroParticipantEvents
) {
    public static MarketingTargetsResponse of(
            long marketingAgreedUsers,
            long dormantUsers,
            long noEventUsers,
            long oneTimeUsers,
            long vipUsers,
            long zeroParticipantEvents
    ) {
        return new MarketingTargetsResponse(
                marketingAgreedUsers, dormantUsers, noEventUsers,
                oneTimeUsers, vipUsers, zeroParticipantEvents
        );
    }
}
