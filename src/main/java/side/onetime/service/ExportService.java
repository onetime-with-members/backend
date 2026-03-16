package side.onetime.service;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import side.onetime.dto.admin.statistics.response.MarketingTargetDetailResponse;
import side.onetime.exception.CustomException;
import side.onetime.global.common.status.ErrorStatus;
import side.onetime.repository.StatisticsRepository;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final StatisticsService statisticsService;
    private final StatisticsRepository statisticsRepository;

    // ==================== 마케팅 CSV ====================

    /**
     * 마케팅 타겟 데이터 조회
     */
    public MarketingTargetDetailResponse getMarketingData(String type) {
        LocalDate defaultStart = LocalDate.of(2020, 1, 1);
        LocalDate defaultEnd = LocalDate.now();
        return switch (type) {
            case "agreed" -> statisticsService.getMarketingAgreedUsers("created_date_desc", null, defaultStart, defaultEnd);
            case "dormant" -> statisticsService.getDormantUsers(30, "created_date_desc", null, defaultStart, defaultEnd);
            case "noEvent" -> statisticsService.getNoEventUsers(7, "created_date_desc", null, defaultStart, defaultEnd);
            case "oneTime" -> statisticsService.getOneTimeUsers("created_date_desc", null, defaultStart, defaultEnd);
            case "vip" -> statisticsService.getVipUsers("created_date_desc", null, defaultStart, defaultEnd);
            case "zeroParticipant" -> statisticsService.getZeroParticipantEvents("created_date_desc", null, defaultStart, defaultEnd);
            default -> throw new CustomException(ErrorStatus._BAD_REQUEST);
        };
    }

    /**
     * 마케팅 유저 CSV 작성
     */
    public void writeMarketingUsersCsv(Writer writer, MarketingTargetDetailResponse data) throws IOException {
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
     * 마케팅 이벤트 CSV 작성
     */
    public void writeMarketingEventsCsv(Writer writer, MarketingTargetDetailResponse data) throws IOException {
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

    // ==================== 유저/이벤트 CSV (경량 쿼리) ====================

    /**
     * CSV 내보내기용 유저 목록 조회
     */
    public List<Object[]> getUsersForCsvExport(String search, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;
        return statisticsRepository.findUsersForCsvExport(search, startDateTime, endDateTime);
    }

    /**
     * CSV 내보내기용 이벤트 목록 조회
     */
    public List<Object[]> getEventsForCsvExport(String search, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;
        return statisticsRepository.findEventsForCsvExport(search, startDateTime, endDateTime);
    }

    /**
     * 유저 CSV 작성
     * Object[]: [users_id, name, email, nickname, provider, language, marketing_policy_agreement, created_date, participation_count]
     */
    public void writeUsersCsv(Writer writer, List<Object[]> users) throws IOException {
        writer.write("ID,Name,Email,Nickname,Provider,Language,Marketing Agreement,Participation Count,Created Date\n");

        if (users == null) return;

        for (var row : users) {
            Long id = row[0] != null ? ((Number) row[0]).longValue() : null;
            String name = row[1] != null ? row[1].toString() : "";
            String email = row[2] != null ? row[2].toString() : "";
            String nickname = row[3] != null ? row[3].toString() : "";
            String provider = row[4] != null ? row[4].toString() : "";
            String language = row[5] != null ? row[5].toString() : "";
            Boolean marketingAgreement = row[6] != null
                    ? (row[6] instanceof Number ? ((Number) row[6]).intValue() == 1 : (Boolean) row[6])
                    : false;
            Object createdDate = row[7];
            Long participationCount = row[8] != null ? ((Number) row[8]).longValue() : 0L;

            writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    id,
                    escapeCsv(name),
                    escapeCsv(email),
                    escapeCsv(nickname),
                    escapeCsv(provider),
                    escapeCsv(language),
                    marketingAgreement ? "Yes" : "No",
                    participationCount,
                    escapeCsv(createdDate)
            ));
        }
    }

    /**
     * 이벤트 CSV 작성
     * Object[]: [events_id, events_uuid, title, category, start_time, end_time, created_date, participant_count]
     */
    public void writeEventsCsv(Writer writer, List<Object[]> events) throws IOException {
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

    // ==================== 유틸 ====================

    private String escapeCsv(Object value) {
        if (value == null) return "";
        String str = value.toString();
        if (!str.isEmpty() && "=+-@\t\r".indexOf(str.charAt(0)) >= 0) {
            str = "'" + str;
        }
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }
}
