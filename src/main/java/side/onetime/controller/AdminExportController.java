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
    private static final DateTimeFormatter DATE_RANGE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");

    /**
     * 마케팅 타겟 CSV 다운로드
     */
    @GetMapping("/marketing/csv")
    public void exportMarketingCsv(
            @RequestParam String type,
            HttpServletResponse response) throws IOException {

        MarketingTargetDetailResponse data = exportService.getMarketingData(type);
        String filename = "marketing_" + type + "_" + LocalDateTime.now().format(FILE_DATE_FORMAT) + ".csv";

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            if ("zeroParticipant".equals(type)) {
                exportService.writeMarketingEventsCsv(writer, data);
            } else {
                exportService.writeMarketingUsersCsv(writer, data);
            }
        }
    }

    /**
     * 유저 목록 CSV 다운로드
     */
    @GetMapping("/users/csv")
    public void exportUsersCsv(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletResponse response) throws IOException {

        List<Object[]> users = exportService.getUsersForCsvExport(search, startDate, endDate);
        String filename = "users_" + buildDateRangeSuffix(startDate, endDate) + ".csv";

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            exportService.writeUsersCsv(writer, users);
        }
    }

    /**
     * 이벤트 목록 CSV 다운로드
     */
    @GetMapping("/events/csv")
    public void exportEventsCsv(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletResponse response) throws IOException {

        List<Object[]> events = exportService.getEventsForCsvExport(search, startDate, endDate);
        String filename = "events_" + buildDateRangeSuffix(startDate, endDate) + ".csv";

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            exportService.writeEventsCsv(writer, events);
        }
    }

    /**
     * 날짜 범위에 따른 파일명 suffix 생성
     */
    private String buildDateRangeSuffix(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return startDate.format(DATE_RANGE_FORMAT) + "_" + endDate.format(DATE_RANGE_FORMAT);
        } else if (startDate != null) {
            return startDate.format(DATE_RANGE_FORMAT) + "_";
        } else if (endDate != null) {
            return "_" + endDate.format(DATE_RANGE_FORMAT);
        }
        return "all";
    }
}
