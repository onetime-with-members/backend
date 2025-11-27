package side.onetime.repository.custom;

import side.onetime.domain.EventParticipation;
import side.onetime.domain.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface EventParticipationRepositoryCustom {
    Map<Long, Integer> countParticipantsByEventIds(List<Long> eventIds);

    List<EventParticipation> findParticipationsByUserWithCursor(User user, LocalDateTime createdDate, int pageSize);
}
