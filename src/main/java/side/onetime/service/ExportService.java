package side.onetime.service;

import java.io.IOException;
import java.io.Writer;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import side.onetime.dto.admin.statistics.response.MarketingTargetDetailResponse;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final StatisticsService statisticsService;

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
}
