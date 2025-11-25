package side.onetime.repository.custom;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import side.onetime.domain.User;
import side.onetime.domain.enums.EventStatus;
import side.onetime.domain.enums.Language;
import side.onetime.domain.enums.Status;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.AdminErrorStatus;
import side.onetime.util.NamingUtil;

import java.time.LocalDateTime;
import java.util.List;

import static side.onetime.domain.QEvent.event;
import static side.onetime.domain.QEventParticipation.eventParticipation;
import static side.onetime.domain.QFixedSelection.fixedSelection;
import static side.onetime.domain.QGuideViewStatus.guideViewStatus;
import static side.onetime.domain.QMember.member;
import static side.onetime.domain.QSchedule.schedule;
import static side.onetime.domain.QSelection.selection;
import static side.onetime.domain.QUser.user;

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 유저 서비스 탈퇴 메서드.
     *
     * 인증된 유저의 계정을 삭제하고,
     * 유저가 생성한(즉, EventParticipation의 상태가 PARTICIPANT가 아닌) 이벤트를 함께 삭제합니다.
     *
     * 삭제 순서:
     * 1. 유저가 생성한 이벤트의 Selection → EventParticipation → Schedule → Member → Event
     * 2. 유저가 직접 소유한 Selection → FixedSelection
     * 3. 최종적으로 User: status를 DELETED로, providerId를 null로 업데이트
     *
     * @param activeUser 탈퇴할 유저
     */
    @Override
    public void withdraw(User activeUser) {
        // 유저가 생성한 이벤트 ID 리스트 조회
        List<Long> eventIds = queryFactory
                .select(eventParticipation.event.id)
                .distinct()
                .from(eventParticipation)
                .where(
                        eventParticipation.user.eq(activeUser)
                                .and(eventParticipation.eventStatus.ne(EventStatus.PARTICIPANT))
                )
                .fetch();
        LocalDateTime deletedTime = LocalDateTime.now();

        if (!eventIds.isEmpty()) {
            queryFactory.delete(selection)
                    .where(selection.schedule.event.id.in(eventIds))
                    .execute();

            queryFactory.delete(eventParticipation)
                    .where(eventParticipation.event.id.in(eventIds))
                    .execute();

            queryFactory.delete(schedule)
                    .where(schedule.event.id.in(eventIds))
                    .execute();

            queryFactory.delete(member)
                    .where(member.event.id.in(eventIds))
                    .execute();

            queryFactory.update(event)
                    .set(event.status, Status.DELETED)
                    .set(event.deletedAt, deletedTime)
                    .where(event.id.in(eventIds))
                    .execute();
        }

        // 유저 소유 Selection, FixedSelection, eventParticipation 삭제
        queryFactory.delete(selection)
                .where(selection.user.eq(activeUser))
                .execute();

        queryFactory.delete(fixedSelection)
                .where(fixedSelection.user.eq(activeUser))
                .execute();

        queryFactory.delete(eventParticipation)
                .where(eventParticipation.user.eq(activeUser))
                .execute();

        queryFactory.delete(guideViewStatus)
                .where(guideViewStatus.user.eq(activeUser))
                .execute();

        queryFactory.update(user)
                .set(user.providerId, Expressions.nullExpression())
                .set(user.status, Status.DELETED)
                .set(user.deletedAt, deletedTime)
                .where(user.eq(activeUser))
                .execute();
    }

    /**
     * 정렬 및 페이징 기능을 포함한 사용자 목록 조회 메서드
     *
     * @param pageable 페이지 정보 (page, size 등)
     * @param keyword 정렬 기준 필드 (ex. id, name, email, created_date, participation_count 등)
     * @param sorting 정렬 방향 (asc 또는 desc)
     * @return 조건에 맞는 사용자 리스트
     */
    @Override
    public List<User> findAllWithSort(Pageable pageable, String keyword, String sorting) {
        Order order = sorting.equalsIgnoreCase("asc") ? Order.ASC : Order.DESC;
        String field = NamingUtil.toCamelCase(keyword);

        JPAQuery<User> query = queryFactory.selectFrom(user);

        if ("participationCount".equals(field)) {
            query
                    .leftJoin(eventParticipation).on(eventParticipation.user.eq(user))
                    .groupBy(user);

            if (order == Order.ASC) {
                query.orderBy(eventParticipation.count().asc());
            } else {
                query.orderBy(eventParticipation.count().desc());
            }

        } else {
            PathBuilder<User> pathBuilder = new PathBuilder<>(User.class, "user");

            OrderSpecifier<?> orderSpecifier = switch (field) {
                case "id" ->
                        new OrderSpecifier<>(order, pathBuilder.getNumber(field, Long.class));
                case "name", "email", "nickname", "provider", "providerId", "sleepStartTime", "sleepEndTime" ->
                        new OrderSpecifier<>(order, pathBuilder.getString(field));
                case "language" ->
                        new OrderSpecifier<>(order, pathBuilder.getEnum(field, Language.class));
                case "createdDate" ->
                        new OrderSpecifier<>(order, pathBuilder.getComparable(field, LocalDateTime.class));
                default -> throw new CustomException(AdminErrorStatus._INVALID_SORT_KEYWORD);
            };

            query.orderBy(orderSpecifier);
        }

        query.offset(pageable.getOffset()).limit(pageable.getPageSize());

        return query.fetch();
    }
}
