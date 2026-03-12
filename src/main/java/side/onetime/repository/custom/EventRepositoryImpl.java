package side.onetime.repository.custom;

import static side.onetime.domain.QEvent.*;
import static side.onetime.domain.QEventParticipation.*;
import static side.onetime.domain.QMember.*;
import static side.onetime.domain.QSchedule.*;
import static side.onetime.domain.QSelection.*;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import side.onetime.domain.Event;
import side.onetime.domain.QEvent;
import side.onetime.domain.QEventParticipation;
import side.onetime.domain.enums.Category;
import side.onetime.domain.enums.EventStatus;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.AdminErrorStatus;
import side.onetime.util.NamingUtil;

@RequiredArgsConstructor
public class EventRepositoryImpl implements EventRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 이벤트 삭제 메서드.
     *
     * 이벤트에 연결된 모든 관련 데이터를 삭제합니다.
     * 삭제 순서는 외래 키 제약 조건을 고려하여,
     * Selection → EventParticipation → Schedule → Member 순으로 삭제 후 Event를 DELETED 처리합니다.
     *
     * @param e 삭제할 Event 객체
     */
    @Override
    public void deleteEvent(Event e) {
        queryFactory.delete(selection)
                .where(selection.schedule.event.eq(e))
                .execute();

        queryFactory.delete(eventParticipation)
                .where(eventParticipation.event.eq(e))
                .execute();

        queryFactory.delete(schedule)
                .where(schedule.event.eq(e))
                .execute();

        queryFactory.delete(member)
                .where(member.event.eq(e))
                .execute();

        queryFactory.update(event)
                .set(event.status, EventStatus.DELETED)
                .set(event.deletedAt, LocalDateTime.now())
                .where(event.eq(e))
                .execute();
    }

    /**
     * 특정 범위에 해당하는 스케줄 삭제 메서드.
     *
     * 이벤트와 연결된 특정 범위(DATE 또는 DAY)에 해당하는 모든 스케줄 및 관련 데이터를 삭제합니다.
     * 삭제 순서는 외래 키 제약 조건을 고려하여,
     * Selection → Schedule 순으로 진행됩니다.
     *
     * @param event 이벤트 객체
     * @param ranges 삭제할 범위 리스트 (DATE 또는 DAY)
     */
    @Override
    public void deleteSchedulesByRanges(Event event, List<String> ranges) {
        if (ranges.isEmpty()) {
            return;
        }

        queryFactory.delete(selection)
                .where(selection.schedule.event.eq(event)
                        .and(selection.schedule.date.in(ranges)
                                .or(selection.schedule.day.in(ranges))))
                .execute();

        queryFactory.delete(schedule)
                .where(schedule.event.eq(event)
                        .and(schedule.date.in(ranges)
                                .or(schedule.day.in(ranges))))
                .execute();
    }

    /**
     * 특정 시간에 해당하는 스케줄 삭제 메서드.
     *
     * 이벤트와 연결된 특정 시간(HH:mm 형식)에 해당하는 모든 스케줄 및 관련 데이터를 삭제합니다.
     * 삭제 순서는 외래 키 제약 조건을 고려하여,
     * Selection → Schedule 순으로 진행됩니다.
     *
     * @param event 이벤트 객체
     * @param times 삭제할 시간 리스트 (HH:mm 형식)
     */
    @Override
    public void deleteSchedulesByTimes(Event event, List<String> times) {
        if (times.isEmpty()) {
            return;
        }

        queryFactory.delete(selection)
                .where(selection.schedule.event.eq(event)
                        .and(selection.schedule.time.in(times)))
                .execute();

        queryFactory.delete(schedule)
                .where(schedule.event.eq(event)
                        .and(schedule.time.in(times)))
                .execute();
    }

    /**
     * 정렬 및 페이징 기능을 포함한 이벤트 조회 메서드
     * keyword: 정렬 필드명(title, category, startTime, endTime, createDate, id, event_id)
     * sorting: asc / desc
     */
    @Override
    public List<Event> findAllWithSort(Pageable pageable, String keyword, String sorting) {
        Order order = sorting.equalsIgnoreCase("asc") ? Order.ASC : Order.DESC;
        String field = NamingUtil.toCamelCase(keyword); // snake_case → camelCase 변환

        QEvent event = QEvent.event;
        QEventParticipation ep = QEventParticipation.eventParticipation;

        JPAQuery<Event> query = queryFactory.selectFrom(event);

        if ("participantCount".equals(field)) {
            query
                    .leftJoin(ep).on(ep.event.eq(event))
                    .groupBy(event);

            if (order == Order.ASC) {
                query.orderBy(ep.count().asc());
            } else {
                query.orderBy(ep.count().desc());
            }

        } else {
            PathBuilder<Event> pathBuilder = new PathBuilder<>(Event.class, "event");

            OrderSpecifier<?> orderSpecifier = switch (field) {
                case "id" ->
                        new OrderSpecifier<>(order, pathBuilder.getNumber("id", Long.class));
                case "eventId" ->
                        new OrderSpecifier<>(order, pathBuilder.getString("eventId")); // eventId는 String 기반
                case "title", "startTime", "endTime" ->
                        new OrderSpecifier<>(order, pathBuilder.getString(field));
                case "category" ->
                        new OrderSpecifier<>(order, pathBuilder.getEnum(field, Category.class));
                case "createdDate" ->
                        new OrderSpecifier<>(order, pathBuilder.getComparable(field, LocalDateTime.class));
                default ->
                        throw new CustomException(AdminErrorStatus._INVALID_SORT_KEYWORD);
            };

            query.orderBy(orderSpecifier);
        }

        query.offset(pageable.getOffset()).limit(pageable.getPageSize());
        return query.fetch();
    }

    /**
     * 검색, 기간 필터를 포함한 이벤트 조회 메서드
     */
    @Override
    public List<Event> findAllWithFilters(Pageable pageable, String sortField, String sorting,
                                           String search, LocalDateTime startDate, LocalDateTime endDate) {
        return findAllWithFilters(pageable, sortField, sorting, search, startDate, endDate, null, null);
    }

    /**
     * 검색, 기간, 시간대, 요일 필터를 포함한 이벤트 조회 메서드
     */
    @Override
    public List<Event> findAllWithFilters(Pageable pageable, String sortField, String sorting,
                                           String search, LocalDateTime startDate, LocalDateTime endDate,
                                           Integer hour, Integer dayOfWeek) {
        Order order = sorting.equalsIgnoreCase("asc") ? Order.ASC : Order.DESC;
        String field = NamingUtil.toCamelCase(sortField);

        QEvent event = QEvent.event;

        JPAQuery<Event> query = queryFactory.selectFrom(event)
                .where(event.status.eq(Status.ACTIVE));

        // 검색 조건
        if (search != null && !search.trim().isEmpty()) {
            query.where(event.title.containsIgnoreCase(search.trim()));
        }

        // 기간 필터 (생성일 기준)
        if (startDate != null) {
            query.where(event.createdDate.goe(startDate));
        }
        if (endDate != null) {
            query.where(event.createdDate.lt(endDate));
        }

        // 시간대 필터 (HOUR)
        if (hour != null) {
            NumberTemplate<Integer> hourExpr = Expressions.numberTemplate(Integer.class,
                    "HOUR({0})", event.createdDate);
            query.where(hourExpr.eq(hour));
        }

        // 요일 필터 (DAYOFWEEK: 1=SUN, 2=MON, ..., 7=SAT)
        if (dayOfWeek != null) {
            NumberTemplate<Integer> dayOfWeekExpr = Expressions.numberTemplate(Integer.class,
                    "DAYOFWEEK({0})", event.createdDate);
            query.where(dayOfWeekExpr.eq(dayOfWeek));
        }

        // 정렬
        PathBuilder<Event> pathBuilder = new PathBuilder<>(Event.class, "event");
        OrderSpecifier<?> orderSpecifier = switch (field) {
            case "id" -> new OrderSpecifier<>(order, pathBuilder.getNumber("id", Long.class));
            case "title", "startTime", "endTime" -> new OrderSpecifier<>(order, pathBuilder.getString(field));
            case "category" -> new OrderSpecifier<>(order, pathBuilder.getEnum(field, Category.class));
            case "createdDate" -> new OrderSpecifier<>(order, pathBuilder.getComparable(field, LocalDateTime.class));
            default -> new OrderSpecifier<>(order, pathBuilder.getComparable("createdDate", LocalDateTime.class));
        };
        query.orderBy(orderSpecifier);

        query.offset(pageable.getOffset()).limit(pageable.getPageSize());
        return query.fetch();
    }

    /**
     * 검색, 기간 필터를 포함한 이벤트 개수 조회
     */
    @Override
    public long countWithFilters(String search, LocalDateTime startDate, LocalDateTime endDate) {
        return countWithFilters(search, startDate, endDate, null, null);
    }

    /**
     * 검색, 기간, 시간대, 요일 필터를 포함한 이벤트 개수 조회
     */
    @Override
    public long countWithFilters(String search, LocalDateTime startDate, LocalDateTime endDate,
                                  Integer hour, Integer dayOfWeek) {
        QEvent event = QEvent.event;

        JPAQuery<Long> query = queryFactory.select(event.count())
                .from(event)
                .where(event.status.eq(Status.ACTIVE));

        if (search != null && !search.trim().isEmpty()) {
            query.where(event.title.containsIgnoreCase(search.trim()));
        }
        if (startDate != null) {
            query.where(event.createdDate.goe(startDate));
        }
        if (endDate != null) {
            query.where(event.createdDate.lt(endDate));
        }

        // 시간대 필터 (HOUR)
        if (hour != null) {
            NumberTemplate<Integer> hourExpr = Expressions.numberTemplate(Integer.class,
                    "HOUR({0})", event.createdDate);
            query.where(hourExpr.eq(hour));
        }

        // 요일 필터 (DAYOFWEEK: 1=SUN, 2=MON, ..., 7=SAT)
        if (dayOfWeek != null) {
            NumberTemplate<Integer> dayOfWeekExpr = Expressions.numberTemplate(Integer.class,
                    "DAYOFWEEK({0})", event.createdDate);
            query.where(dayOfWeekExpr.eq(dayOfWeek));
        }

        Long count = query.fetchOne();
        return count != null ? count : 0;
    }
}
