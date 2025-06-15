package side.onetime.repository.custom;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import side.onetime.domain.*;
import side.onetime.domain.enums.EventStatus;
import side.onetime.domain.enums.Language;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.AdminErrorStatus;
import side.onetime.util.NamingUtil;

import java.time.LocalDateTime;
import java.util.List;

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
     * 3. 최종적으로 User
     *
     * @param user 탈퇴할 유저
     */
    @Override
    public void withdraw(User user) {
        // 유저가 생성한 이벤트 ID 리스트 조회
        List<Long> eventIds = queryFactory
                .select(QEventParticipation.eventParticipation.event.id)
                .distinct()
                .from(QEventParticipation.eventParticipation)
                .where(
                        QEventParticipation.eventParticipation.user.eq(user)
                                .and(QEventParticipation.eventParticipation.eventStatus.ne(EventStatus.PARTICIPANT))
                )
                .fetch();

        if (!eventIds.isEmpty()) {
            queryFactory.delete(QSelection.selection)
                    .where(QSelection.selection.schedule.event.id.in(eventIds))
                    .execute();

            queryFactory.delete(QEventParticipation.eventParticipation)
                    .where(QEventParticipation.eventParticipation.event.id.in(eventIds))
                    .execute();

            queryFactory.delete(QSchedule.schedule)
                    .where(QSchedule.schedule.event.id.in(eventIds))
                    .execute();

            queryFactory.delete(QMember.member)
                    .where(QMember.member.event.id.in(eventIds))
                    .execute();

            queryFactory.delete(QEvent.event)
                    .where(QEvent.event.id.in(eventIds))
                    .execute();
        }

        // 유저 소유 Selection, FixedSelection, eventParticipation 삭제
        queryFactory.delete(QSelection.selection)
                .where(QSelection.selection.user.eq(user))
                .execute();

        queryFactory.delete(QFixedSelection.fixedSelection)
                .where(QFixedSelection.fixedSelection.user.eq(user))
                .execute();

        queryFactory.delete(QEventParticipation.eventParticipation)
                .where(QEventParticipation.eventParticipation.user.eq(user))
                .execute();

        // 최종적으로 유저 삭제
        queryFactory.delete(QUser.user)
                .where(QUser.user.eq(user))
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

        QUser user = QUser.user;
        QEventParticipation ep = QEventParticipation.eventParticipation;

        JPAQuery<User> query = queryFactory.selectFrom(user);

        if ("participationCount".equals(field)) {
            query
                    .leftJoin(ep).on(ep.user.eq(user))
                    .groupBy(user);

            if (order == Order.ASC) {
                query.orderBy(ep.count().asc());
            } else {
                query.orderBy(ep.count().desc());
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
