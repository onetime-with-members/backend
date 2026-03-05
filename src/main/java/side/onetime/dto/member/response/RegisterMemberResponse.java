package side.onetime.dto.member.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import side.onetime.domain.Event;
import side.onetime.domain.Member;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RegisterMemberResponse(
        String memberId,
        String category
) {
    public static RegisterMemberResponse of(Member member, Event event) {
        return new RegisterMemberResponse(
                String.valueOf(member.getMemberId()),
                event.getCategory().name()
        );
    }
}
