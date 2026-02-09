package side.onetime.repository.custom;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import side.onetime.domain.EmailLog;
import side.onetime.domain.QEmailLog;
import side.onetime.domain.enums.EmailLogStatus;

@Repository
@RequiredArgsConstructor
public class EmailLogRepositoryImpl implements EmailLogRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QEmailLog emailLog = QEmailLog.emailLog;

    @Override
    public List<EmailLog> findAllWithFilters(Pageable pageable, String search,
                                              LocalDateTime startDate, LocalDateTime endDate,
                                              String status, String targetGroup) {
        JPAQuery<EmailLog> query = queryFactory.selectFrom(emailLog);

        applyFilters(query, search, startDate, endDate, status, targetGroup);

        query.orderBy(emailLog.sentAt.desc());
        query.offset(pageable.getOffset()).limit(pageable.getPageSize());
        return query.fetch();
    }

    @Override
    public long countWithFilters(String search, LocalDateTime startDate, LocalDateTime endDate,
                                  String status, String targetGroup) {
        JPAQuery<Long> query = queryFactory.select(emailLog.count()).from(emailLog);

        applyFilters(query, search, startDate, endDate, status, targetGroup);

        Long count = query.fetchOne();
        return count != null ? count : 0;
    }

    private void applyFilters(JPAQuery<?> query, String search,
                               LocalDateTime startDate, LocalDateTime endDate,
                               String status, String targetGroup) {
        // 검색 (수신자 OR 제목)
        if (search != null && !search.trim().isEmpty()) {
            String searchTerm = search.trim();
            query.where(
                    emailLog.recipient.containsIgnoreCase(searchTerm)
                            .or(emailLog.subject.containsIgnoreCase(searchTerm))
            );
        }

        // 날짜 범위 필터
        if (startDate != null) {
            query.where(emailLog.sentAt.goe(startDate));
        }
        if (endDate != null) {
            query.where(emailLog.sentAt.lt(endDate));
        }

        // 상태 필터
        if (status != null && !status.isBlank()) {
            try {
                EmailLogStatus emailLogStatus = EmailLogStatus.valueOf(status.toUpperCase());
                query.where(emailLog.status.eq(emailLogStatus));
            } catch (IllegalArgumentException ignored) {
                // 잘못된 status 값은 무시
            }
        }

        // 타겟 그룹 필터
        if (targetGroup != null && !targetGroup.isBlank()) {
            query.where(emailLog.targetGroup.eq(targetGroup));
        }
    }
}
