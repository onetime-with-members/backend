package side.onetime.controller;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import side.onetime.dto.admin.response.DashboardUser;
import side.onetime.dto.admin.statistics.response.MarketingTargetDetailResponse;
import side.onetime.service.ExportService;

/**
 * 어드민 데이터 내보내기 컨트롤러
 */
@Hidden
@RestController
@RequestMapping("/api/v1/admin/export")
@RequiredArgsConstructor
public class AdminExportController {

    private final ExportService exportService;

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * 마케팅 타겟 CSV 다운로드
     *
     * @param type 마케팅 타겟 유형 (agreed, dormant, noEvent, oneTime, vip, zeroParticipant)
     * @param limit 최대 개수
     * @param response HTTP 응답
     */
    @GetMapping("/marketing/csv")
    public void exportMarketingTargetCsv(
            @RequestParam String type,
            @RequestParam(defaultValue = "1000") int limit,
            HttpServletResponse response) throws IOException {

        MarketingTargetDetailResponse data = exportService.getMarketingData(type, limit);
        String filename = "marketing_" + type + "_" + LocalDateTime.now().format(FILE_DATE_FORMAT) + ".csv";

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // BOM for Excel UTF-8 compatibility
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            if ("zeroParticipant".equals(type)) {
                exportService.writeEventsCsv(writer, data);
            } else {
                exportService.writeUsersCsv(writer, data);
            }
        }
    }

    /**
     * 유저 목록 CSV 다운로드
     *
     * @param keyword 정렬 기준 (created_date, name)
     * @param sorting 정렬 방향 (asc, desc)
     * @param search 검색어
     * @param startDate 시작일 (가입일 기준)
     * @param endDate 종료일 (가입일 기준)
     * @param limit 최대 개수
     * @param response HTTP 응답
     */
    @GetMapping("/users/csv")
    public void exportUsersCsv(
            @RequestParam(defaultValue = "created_date") String keyword,
            @RequestParam(defaultValue = "desc") String sorting,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "10000") int limit,
            HttpServletResponse response) throws IOException {

        List<DashboardUser> users = exportService.getAllUsersForExport(keyword, sorting, search, startDate, endDate, limit);
        String filename = "users_" + LocalDateTime.now().format(FILE_DATE_FORMAT) + ".csv";

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // BOM for Excel UTF-8 compatibility
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            exportService.writeDashboardUsersCsv(writer, users);
        }
    }

    /**
     * 이벤트 목록 CSV 다운로드
     *
     * @param keyword 정렬 기준 (created_date, title, participant_count)
     * @param sorting 정렬 방향 (asc, desc)
     * @param search 검색어
     * @param startDate 시작일 (생성일 기준)
     * @param endDate 종료일 (생성일 기준)
     * @param limit 최대 개수
     * @param response HTTP 응답
     */
    @GetMapping("/events/csv")
    public void exportEventsCsv(
            @RequestParam(defaultValue = "created_date") String keyword,
            @RequestParam(defaultValue = "desc") String sorting,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "1000") int limit,
            HttpServletResponse response) throws IOException {

        List<Object[]> events = exportService.getEventsForCsvExport(keyword, sorting, search, startDate, endDate, limit);
        String filename = "events_" + LocalDateTime.now().format(FILE_DATE_FORMAT) + ".csv";

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // BOM for Excel UTF-8 compatibility
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            exportService.writeEventsCsvLightweight(writer, events);
        }
    }
}
