package side.onetime.dto.event.response;

import side.onetime.domain.Member;
import side.onetime.domain.User;

import java.util.List;

public record GetParticipantsResponse(
        List<Participant> members,
        List<Participant> users
) {
    public record Participant(
            long id,
            String name
    ) {
        public static Participant of(long id, String name) {
            return new Participant(id, name);
        }
    }

    public static GetParticipantsResponse of(List<Member> members, List<User> users) {
        List<Participant> memberList = members.stream()
                .map(member -> Participant.of(member.getId(), member.getName()))
                .toList();

        List<Participant> userList = users.stream()
                .map(user -> Participant.of(user.getId(), user.getNickname()))
                .toList();

        return new GetParticipantsResponse(memberList, userList);
    }
}
