package side.onetime.dto.member.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import side.onetime.domain.Event;
import side.onetime.domain.Member;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LoginMemberResponse(
        String memberId,
        String category
) {
    public static LoginMemberResponse of(Member member, Event event) {
        return new LoginMemberResponse(
                String.valueOf(member.getMemberId()),
                event.getCategory().name()
        );
    }
}
