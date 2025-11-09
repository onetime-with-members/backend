package side.onetime.repository.custom;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import side.onetime.domain.EventParticipation;
import side.onetime.domain.QEvent;
import side.onetime.domain.QEventParticipation;
import side.onetime.domain.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class EventParticipationRepositoryImpl implements EventParticipationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 이벤트 ID 리스트를 기반으로 각 이벤트별 참여자 수를 조회합니다.
     *
     * @param eventIds 이벤트 식별자 리스트 (eventId - String)
     * @return 각 이벤트 ID에 대응하는 참여자 수 Map
     */
    @Override
    public Map<Long, Integer> countParticipantsByEventIds(List<Long> eventIds) {
        QEventParticipation ep = QEventParticipation.eventParticipation;
        QEvent e = QEvent.event;

        return queryFactory
                .select(e.id, ep.id.count())
                .from(ep)
                .join(ep.event, e)
                .where(e.id.in(eventIds))
                .groupBy(e.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(e.id),
                        tuple -> Math.toIntExact(tuple.get(ep.id.count()))
                ));
    }

    /**
     * 유저가 참여한 이벤트를 생성일(createdDate) 기준으로 페이지 단위로 조회합니다.
     *
     * @param user 조회할 유저 객체
     * @param createdDate 조회 기준 생성일
     * @param pageSize 한 번에 조회할 이벤트 수
     * @return 이벤트 참여 목록
     */
    @Override
    public List<EventParticipation> findParticipationsByUserWithCursor(User user, LocalDateTime createdDate, int pageSize) {
        QEventParticipation ep = QEventParticipation.eventParticipation;
        QEvent e = QEvent.event;

        return queryFactory
                .select(ep)
                .from(ep)
                .join(ep.event, e).fetchJoin()
                .where(
                        ep.user.eq(user),
                        ltCreatedDate(createdDate)
                )
                .orderBy(e.createdDate.desc())
                .limit(pageSize)
                .fetch();
    }

    /**
     * 주어진 createdDate 이전에 생성된 이벤트만 필터링하는 BooleanExpression을 반환합니다.
     *
     * @param createdDate 기준 생성일
     * @return createdDate 이전의 이벤트를 조회하는 BooleanExpression
     */
    private BooleanExpression ltCreatedDate(LocalDateTime createdDate) {
        QEventParticipation ep = QEventParticipation.eventParticipation;

        if (createdDate == null) {
            return null;
        }
        return ep.event.createdDate.lt(createdDate);
    }
}
