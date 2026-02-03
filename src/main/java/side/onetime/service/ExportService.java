package side.onetime.service;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import side.onetime.dto.admin.response.DashboardEvent;
import side.onetime.dto.admin.response.DashboardUser;
import side.onetime.dto.admin.response.GetAllDashboardEventsResponse;
import side.onetime.dto.admin.response.GetAllDashboardUsersResponse;
import side.onetime.dto.admin.statistics.response.MarketingTargetDetailResponse;
import side.onetime.repository.StatisticsRepository;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final StatisticsService statisticsService;
    private final AdminService adminService;
    private final StatisticsRepository statisticsRepository;

    /**
     * 마케팅 타겟 데이터 조회
     */
    public MarketingTargetDetailResponse getMarketingData(String type, int limit) {
        return switch (type) {
            case "agreed" -> statisticsService.getMarketingAgreedUsers(limit, "created_date_desc", null);
            case "dormant" -> statisticsService.getDormantUsers(30, limit, "created_date_desc", null);
            case "noEvent" -> statisticsService.getNoEventUsers(7, limit, "created_date_desc", null);
            case "oneTime" -> statisticsService.getOneTimeUsers(limit, "created_date_desc", null);
            case "vip" -> statisticsService.getVipUsers(limit, "created_date_desc", null);
            case "zeroParticipant" -> statisticsService.getZeroParticipantEvents(limit, "created_date_desc", null);
            default -> throw new IllegalArgumentException("Invalid type: " + type);
        };
    }

    /**
     * 유저 목록 CSV 작성
     */
    public void writeUsersCsv(Writer writer, MarketingTargetDetailResponse data) throws IOException {
        writer.write("User ID,Email,Name,Nickname,Provider,Language,Marketing Agreement,Created Date,Extra Info\n");

        if (data.users() == null) return;

        for (var user : data.users()) {
            String extra = "";
            if (user.daysInactive() != null) {
                extra = user.daysInactive() + " days inactive";
            } else if (user.daysSinceSignup() != null) {
                extra = user.daysSinceSignup() + " days since signup";
            } else if (user.eventCount() != null) {
                extra = user.eventCount() + " events";
            }

            writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    escapeCsv(user.userId()),
                    escapeCsv(user.email()),
                    escapeCsv(user.name()),
                    escapeCsv(user.nickname()),
                    escapeCsv(user.provider()),
                    escapeCsv(user.language()),
                    user.marketingPolicyAgreement() != null && user.marketingPolicyAgreement() ? "Yes" : "No",
                    escapeCsv(user.createdDate()),
                    escapeCsv(extra)
            ));
        }
    }

    /**
     * 이벤트 목록 CSV 작성
     */
    public void writeEventsCsv(Writer writer, MarketingTargetDetailResponse data) throws IOException {
        writer.write("Event ID,Title,Category,Creator Name,Creator Email,Created Date,Days Since Created\n");

        if (data.events() == null) return;

        for (var event : data.events()) {
            writer.write(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                    escapeCsv(event.eventId()),
                    escapeCsv(event.title()),
                    escapeCsv(event.category()),
                    escapeCsv(event.creatorName()),
                    escapeCsv(event.creatorEmail()),
                    escapeCsv(event.createdDate()),
                    event.daysSinceCreated() != null ? event.daysSinceCreated() : ""
            ));
        }
    }

    private String escapeCsv(Object value) {
        if (value == null) return "";
        String str = value.toString();
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

    /**
     * 유저 목록 데이터 조회 (페이지네이션 없이 전체)
     */
    public List<DashboardUser> getAllUsersForExport(
            String keyword, String sorting, String search,
            LocalDate startDate, LocalDate endDate, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        GetAllDashboardUsersResponse response = adminService.getAllDashboardUsers(
                pageable, keyword, sorting, search, startDate, endDate);
        return response.dashboardUsers();
    }

    /**
     * 대시보드 유저 목록 CSV 작성
     */
    public void writeDashboardUsersCsv(Writer writer, List<DashboardUser> users) throws IOException {
        writer.write("ID,Name,Email,Nickname,Provider,Language,Marketing Agreement,Participation Count,Created Date\n");

        if (users == null) return;

        for (var user : users) {
            writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    user.id(),
                    escapeCsv(user.name()),
                    escapeCsv(user.email()),
                    escapeCsv(user.nickname()),
                    escapeCsv(user.provider()),
                    user.language() != null ? user.language().name() : "",
                    user.marketingPolicyAgreement() != null && user.marketingPolicyAgreement() ? "Yes" : "No",
                    user.participantCount(),
                    escapeCsv(user.createdDate())
            ));
        }
    }

    /**
     * 이벤트 목록 데이터 조회 (페이지네이션 없이 전체)
     */
    public List<DashboardEvent> getAllEventsForExport(
            String keyword, String sorting, String search,
            LocalDate startDate, LocalDate endDate, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        GetAllDashboardEventsResponse response = adminService.getAllDashboardEvents(
                pageable, keyword, sorting, search, startDate, endDate);
        return response.dashboardEvents();
    }

    /**
     * 대시보드 이벤트 목록 CSV 작성
     */
    public void writeDashboardEventsCsv(Writer writer, List<DashboardEvent> events) throws IOException {
        writer.write("ID,Event ID,Title,Category,Date Range,Time Range,Participant Count,Created Date\n");

        if (events == null) return;

        for (var event : events) {
            writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s\n",
                    event.id(),
                    escapeCsv(event.eventId()),
                    escapeCsv(event.title()),
                    event.category() != null ? event.category().name() : "",
                    escapeCsv(event.dateRange()),
                    escapeCsv(event.timeRange()),
                    event.participantCount(),
                    escapeCsv(event.createdDate())
            ));
        }
    }

    /**
     * CSV 내보내기용 이벤트 목록 조회 (경량 버전)
     * 스케줄 조회 없이 기본 정보만 가져옴 - 훨씬 빠름
     */
    public List<Object[]> getEventsForCsvExport(
            String keyword, String sorting, String search,
            LocalDate startDate, LocalDate endDate, int limit) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

        return statisticsRepository.findEventsForCsvExport(search, startDateTime, endDateTime, limit);
    }

    /**
     * CSV 내보내기용 경량 이벤트 목록 작성
     * Object[] 형식: [events_id, event_id, title, category, start_time, end_time, created_date, participant_count]
     */
    public void writeEventsCsvLightweight(Writer writer, List<Object[]> events) throws IOException {
        writer.write("ID,Event ID,Title,Category,Time Range,Participant Count,Created Date\n");

        if (events == null) return;

        for (var row : events) {
            Long id = row[0] != null ? ((Number) row[0]).longValue() : null;
            String eventId = row[1] != null ? row[1].toString() : "";
            String title = row[2] != null ? row[2].toString() : "";
            String category = row[3] != null ? row[3].toString() : "";
            String startTime = row[4] != null ? row[4].toString() : "";
            String endTime = row[5] != null ? row[5].toString() : "";
            String timeRange = startTime + " - " + endTime;
            Object createdDate = row[6];
            Long participantCount = row[7] != null ? ((Number) row[7]).longValue() : 0L;

            writer.write(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                    id,
                    escapeCsv(eventId),
                    escapeCsv(title),
                    escapeCsv(category),
                    escapeCsv(timeRange),
                    participantCount,
                    escapeCsv(createdDate)
            ));
        }
    }
}
