package side.onetime.controller;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
@RequestMapping("/admin/api/export")
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
}
