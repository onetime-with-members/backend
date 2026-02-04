package side.onetime.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import side.onetime.configuration.AdminControllerTestConfig;
import side.onetime.controller.AdminExportController;
import side.onetime.dto.admin.statistics.response.MarketingTargetDetailResponse;
import side.onetime.service.ExportService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminExportController.class)
public class AdminExportControllerTest extends AdminControllerTestConfig {

    @MockBean
    private ExportService exportService;

    // ==================== Marketing CSV Export ====================

    @Test
    @DisplayName("마케팅 동의 유저 CSV를 다운로드한다")
    public void exportMarketingAgreedCsv() throws Exception {
        // given
        List<MarketingTargetDetailResponse.UserDetail> users = List.of(
                MarketingTargetDetailResponse.UserDetail.builder()
                        .userId(1L)
                        .name("홍길동")
                        .email("hong@example.com")
                        .nickname("길동이")
                        .provider("KAKAO")
                        .marketingPolicyAgreement(true)
                        .createdDate(LocalDateTime.of(2025, 3, 1, 10, 0))
                        .build()
        );
        MarketingTargetDetailResponse response = MarketingTargetDetailResponse.ofUsers("agreed", 1, users);

        // when
        Mockito.when(exportService.getMarketingData("agreed")).thenReturn(response);
        Mockito.doNothing().when(exportService).writeMarketingUsersCsv(any(), any());

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/export/marketing/csv")
                        .param("type", "agreed"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("marketing_agreed_")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".csv")));

        Mockito.verify(exportService).getMarketingData("agreed");
        Mockito.verify(exportService).writeMarketingUsersCsv(any(), eq(response));
    }

    @Test
    @DisplayName("휴면 유저 CSV를 다운로드한다")
    public void exportMarketingDormantCsv() throws Exception {
        // given
        List<MarketingTargetDetailResponse.UserDetail> users = List.of(
                MarketingTargetDetailResponse.UserDetail.builder()
                        .userId(1L)
                        .name("김철수")
                        .email("kim@example.com")
                        .daysInactive(45)
                        .build()
        );
        MarketingTargetDetailResponse response = MarketingTargetDetailResponse.ofUsers("dormant", 1, users);

        // when
        Mockito.when(exportService.getMarketingData("dormant")).thenReturn(response);
        Mockito.doNothing().when(exportService).writeMarketingUsersCsv(any(), any());

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/export/marketing/csv")
                        .param("type", "dormant"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("marketing_dormant_")));

        Mockito.verify(exportService).getMarketingData("dormant");
    }

    @Test
    @DisplayName("참여자 없는 이벤트 CSV를 다운로드한다")
    public void exportZeroParticipantEventsCsv() throws Exception {
        // given
        List<MarketingTargetDetailResponse.EventDetail> events = List.of(
                MarketingTargetDetailResponse.EventDetail.builder()
                        .eventId(1L)
                        .title("팀 미팅")
                        .category("DATE")
                        .creatorName("홍길동")
                        .creatorEmail("hong@example.com")
                        .createdDate(LocalDateTime.of(2025, 3, 1, 10, 0))
                        .daysSinceCreated(10)
                        .build()
        );
        MarketingTargetDetailResponse response = MarketingTargetDetailResponse.ofEvents("zeroParticipant", 1, events);

        // when
        Mockito.when(exportService.getMarketingData("zeroParticipant")).thenReturn(response);
        Mockito.doNothing().when(exportService).writeMarketingEventsCsv(any(), any());

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/export/marketing/csv")
                        .param("type", "zeroParticipant"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("marketing_zeroParticipant_")));

        Mockito.verify(exportService).getMarketingData("zeroParticipant");
        Mockito.verify(exportService).writeMarketingEventsCsv(any(), eq(response));
    }

    // ==================== Users CSV Export ====================

    @Test
    @DisplayName("유저 목록 CSV를 다운로드한다 (전체)")
    public void exportUsersCsvAll() throws Exception {
        // given
        List<Object[]> users = new ArrayList<>();
        users.add(new Object[]{1L, "홍길동", "hong@example.com", "길동이", "KAKAO", "KOR", true, LocalDateTime.of(2025, 3, 1, 10, 0), 5L});

        // when
        Mockito.when(exportService.getUsersForCsvExport(isNull(), isNull(), isNull())).thenReturn(users);
        Mockito.doNothing().when(exportService).writeUsersCsv(any(), any());

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/export/users/csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("users_all.csv")));

        Mockito.verify(exportService).getUsersForCsvExport(isNull(), isNull(), isNull());
        Mockito.verify(exportService).writeUsersCsv(any(), eq(users));
    }

    @Test
    @DisplayName("유저 목록 CSV를 다운로드한다 (검색 조건)")
    public void exportUsersCsvWithSearch() throws Exception {
        // given
        List<Object[]> users = new ArrayList<>();
        users.add(new Object[]{1L, "홍길동", "hong@example.com", "길동이", "KAKAO", "KOR", true, LocalDateTime.of(2025, 3, 1, 10, 0), 5L});

        // when
        Mockito.when(exportService.getUsersForCsvExport(eq("홍길동"), isNull(), isNull())).thenReturn(users);
        Mockito.doNothing().when(exportService).writeUsersCsv(any(), any());

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/export/users/csv")
                        .param("search", "홍길동"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv; charset=UTF-8"));

        Mockito.verify(exportService).getUsersForCsvExport(eq("홍길동"), isNull(), isNull());
    }

    @Test
    @DisplayName("유저 목록 CSV를 다운로드한다 (날짜 범위)")
    public void exportUsersCsvWithDateRange() throws Exception {
        // given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);
        List<Object[]> users = new ArrayList<>();
        users.add(new Object[]{1L, "홍길동", "hong@example.com", "길동이", "KAKAO", "KOR", true, LocalDateTime.of(2025, 3, 1, 10, 0), 5L});

        // when
        Mockito.when(exportService.getUsersForCsvExport(isNull(), eq(startDate), eq(endDate))).thenReturn(users);
        Mockito.doNothing().when(exportService).writeUsersCsv(any(), any());

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/export/users/csv")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("users_250101_250331.csv")));

        Mockito.verify(exportService).getUsersForCsvExport(isNull(), eq(startDate), eq(endDate));
    }

    // ==================== Events CSV Export ====================

    @Test
    @DisplayName("이벤트 목록 CSV를 다운로드한다 (전체)")
    public void exportEventsCsvAll() throws Exception {
        // given
        List<Object[]> events = new ArrayList<>();
        events.add(new Object[]{1L, "event-uuid-1", "팀 미팅", "DATE", "09:00", "18:00", LocalDateTime.of(2025, 3, 1, 10, 0), 5L});

        // when
        Mockito.when(exportService.getEventsForCsvExport(isNull(), isNull(), isNull())).thenReturn(events);
        Mockito.doNothing().when(exportService).writeEventsCsv(any(), any());

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/export/events/csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("events_all.csv")));

        Mockito.verify(exportService).getEventsForCsvExport(isNull(), isNull(), isNull());
        Mockito.verify(exportService).writeEventsCsv(any(), eq(events));
    }

    @Test
    @DisplayName("이벤트 목록 CSV를 다운로드한다 (검색 조건)")
    public void exportEventsCsvWithSearch() throws Exception {
        // given
        List<Object[]> events = new ArrayList<>();
        events.add(new Object[]{1L, "event-uuid-1", "팀 미팅", "DATE", "09:00", "18:00", LocalDateTime.of(2025, 3, 1, 10, 0), 5L});

        // when
        Mockito.when(exportService.getEventsForCsvExport(eq("팀 미팅"), isNull(), isNull())).thenReturn(events);
        Mockito.doNothing().when(exportService).writeEventsCsv(any(), any());

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/export/events/csv")
                        .param("search", "팀 미팅"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv; charset=UTF-8"));

        Mockito.verify(exportService).getEventsForCsvExport(eq("팀 미팅"), isNull(), isNull());
    }

    @Test
    @DisplayName("이벤트 목록 CSV를 다운로드한다 (날짜 범위)")
    public void exportEventsCsvWithDateRange() throws Exception {
        // given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);
        List<Object[]> events = new ArrayList<>();
        events.add(new Object[]{1L, "event-uuid-1", "팀 미팅", "DATE", "09:00", "18:00", LocalDateTime.of(2025, 3, 1, 10, 0), 5L});

        // when
        Mockito.when(exportService.getEventsForCsvExport(isNull(), eq(startDate), eq(endDate))).thenReturn(events);
        Mockito.doNothing().when(exportService).writeEventsCsv(any(), any());

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/export/events/csv")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("events_250101_250331.csv")));

        Mockito.verify(exportService).getEventsForCsvExport(isNull(), eq(startDate), eq(endDate));
    }
}
