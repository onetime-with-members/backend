package side.onetime.repository.custom;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 통계 Repository 커스텀 인터페이스
 * 정렬, 검색 기능을 지원하는 동적 쿼리
 */
public interface StatisticsRepositoryCustom {

    /**
     * 마케팅 동의 유저 목록 (정렬, 검색 지원)
     */
    List<Object[]> findMarketingAgreedUserDetailsWithSortAndSearch(int limit, String sort, String search);

    /**
     * 휴면 유저 목록 (정렬, 검색, 기간 필터 지원)
     */
    List<Object[]> findDormantUserDetailsWithSortAndSearch(int days, int limit, String sort, String search,
                                                           LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 이벤트 미생성 유저 목록 (정렬, 검색 지원)
     */
    List<Object[]> findNoEventUserDetailsWithSortAndSearch(int daysAfterSignup, int limit, String sort, String search);

    /**
     * 1회성 유저 목록 (정렬, 검색 지원)
     */
    List<Object[]> findOneTimeUserDetailsWithSortAndSearch(int limit, String sort, String search);

    /**
     * VIP 유저 목록 (정렬, 검색 지원)
     */
    List<Object[]> findVipUserDetailsWithSortAndSearch(int limit, String sort, String search);

    /**
     * 참여자 0명 이벤트 목록 (정렬, 검색 지원)
     */
    List<Object[]> findZeroParticipantEventDetailsWithSortAndSearch(int limit, String sort, String search);
}
