package side.onetime.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import side.onetime.configuration.AdminControllerTestConfig;
import side.onetime.controller.AdminEmailController;
import side.onetime.domain.enums.EmailLogStatus;
import side.onetime.dto.admin.email.request.CreateEmailTemplateRequest;
import side.onetime.dto.admin.email.request.SendByTemplateRequest;
import side.onetime.dto.admin.email.request.SendEmailRequest;
import side.onetime.dto.admin.email.request.SendToGroupRequest;
import side.onetime.dto.admin.email.request.UpdateEmailTemplateRequest;
import side.onetime.dto.admin.email.response.EmailLogPageResponse;
import side.onetime.dto.admin.email.response.EmailLogResponse;
import side.onetime.dto.admin.email.response.EmailLogStatsResponse;
import side.onetime.dto.admin.email.response.EmailTemplateResponse;
import side.onetime.dto.admin.email.response.SendEmailResponse;
import side.onetime.service.EmailService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminEmailController.class)
public class AdminEmailControllerTest extends AdminControllerTestConfig {

    @MockBean
    private EmailService emailService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== Email Sending ====================

    @Test
    @DisplayName("단일/다중 이메일을 발송한다")
    public void sendEmail() throws Exception {
        // given
        SendEmailRequest request = new SendEmailRequest(
                List.of("hong@example.com", "kim@example.com"),
                "테스트 제목",
                "테스트 내용입니다.",
                "TEXT",
                List.of(1L, 2L)
        );
        SendEmailResponse response = SendEmailResponse.success(2);

        // when
        Mockito.when(emailService.sendEmail(any(SendEmailRequest.class))).thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/admin/email/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.success").value(true))
                .andExpect(jsonPath("$.payload.sentCount").value(2))
                .andExpect(jsonPath("$.payload.failedCount").value(0));

        Mockito.verify(emailService).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    @DisplayName("마케팅 그룹에 이메일을 발송한다")
    public void sendToMarketingGroup() throws Exception {
        // given
        SendToGroupRequest request = new SendToGroupRequest(
                "agreed",
                "마케팅 이메일",
                "안녕하세요, OneTime 입니다.",
                "TEXT",
                100
        );
        SendEmailResponse response = SendEmailResponse.success(50);

        // when
        Mockito.when(emailService.sendToMarketingGroup(any(SendToGroupRequest.class))).thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/admin/email/send-to-group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.success").value(true))
                .andExpect(jsonPath("$.payload.sentCount").value(50));

        Mockito.verify(emailService).sendToMarketingGroup(any(SendToGroupRequest.class));
    }

    @Test
    @DisplayName("템플릿으로 이메일을 발송한다")
    public void sendByTemplate() throws Exception {
        // given
        SendByTemplateRequest request = new SendByTemplateRequest(
                "WELCOME",
                List.of("hong@example.com"),
                List.of(1L)
        );
        SendEmailResponse response = SendEmailResponse.success(1);

        // when
        Mockito.when(emailService.sendByTemplate(any(SendByTemplateRequest.class))).thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/admin/email/send-by-template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.success").value(true))
                .andExpect(jsonPath("$.payload.sentCount").value(1));

        Mockito.verify(emailService).sendByTemplate(any(SendByTemplateRequest.class));
    }

    @Test
    @DisplayName("이메일 발송 일부 실패 시 결과를 반환한다")
    public void sendEmailPartialFailure() throws Exception {
        // given
        SendEmailRequest request = new SendEmailRequest(
                List.of("hong@example.com", "invalid@invalid.invalid"),
                "테스트 제목",
                "테스트 내용입니다.",
                "TEXT",
                List.of(1L, 2L)
        );
        SendEmailResponse response = SendEmailResponse.of(1, 1, List.of("invalid@invalid.invalid"));

        // when
        Mockito.when(emailService.sendEmail(any(SendEmailRequest.class))).thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/admin/email/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.success").value(false))
                .andExpect(jsonPath("$.payload.sentCount").value(1))
                .andExpect(jsonPath("$.payload.failedCount").value(1))
                .andExpect(jsonPath("$.payload.failedEmails[0]").value("invalid@invalid.invalid"));
    }

    // ==================== Email Logs ====================

    @Test
    @DisplayName("이메일 발송 로그를 조회한다")
    public void getEmailLogs() throws Exception {
        // given
        List<EmailLogResponse> logs = List.of(
                new EmailLogResponse(
                        1L, 1L, "hong@example.com", "테스트 제목", null,
                        "TEXT", EmailLogStatus.SENT, null, null,
                        LocalDateTime.of(2025, 3, 1, 10, 0, 0)
                ),
                new EmailLogResponse(
                        2L, 2L, "kim@example.com", "테스트 제목", null,
                        "TEXT", EmailLogStatus.SENT, null, "agreed",
                        LocalDateTime.of(2025, 3, 1, 10, 1, 0)
                )
        );
        EmailLogPageResponse response = new EmailLogPageResponse(logs, 0, 1, 2, false, false);

        // when
        Mockito.when(emailService.getEmailLogs(anyInt(), anyInt(), any(), any(), any(), any(), any()))
                .thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/email/logs")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.logs").isArray())
                .andExpect(jsonPath("$.payload.logs[0].recipient").value("hong@example.com"))
                .andExpect(jsonPath("$.payload.totalElements").value(2));

        Mockito.verify(emailService).getEmailLogs(eq(0), eq(20), isNull(), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    @DisplayName("이메일 발송 로그를 필터링하여 조회한다")
    public void getEmailLogsWithFilter() throws Exception {
        // given
        List<EmailLogResponse> logs = List.of(
                new EmailLogResponse(
                        3L, 3L, "fail@example.com", "실패 메일", null,
                        "TEXT", EmailLogStatus.FAILED, "SMTP error", null,
                        LocalDateTime.of(2025, 3, 1, 10, 2, 0)
                )
        );
        EmailLogPageResponse response = new EmailLogPageResponse(logs, 0, 1, 1, false, false);

        // when
        Mockito.when(emailService.getEmailLogs(anyInt(), anyInt(), eq("FAILED"), any(), any(), any(), any()))
                .thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/email/logs")
                        .param("page", "0")
                        .param("size", "20")
                        .param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.logs[0].status").value("FAILED"))
                .andExpect(jsonPath("$.payload.logs[0].errorMessage").value("SMTP error"));

        Mockito.verify(emailService).getEmailLogs(eq(0), eq(20), eq("FAILED"), isNull(), isNull(), isNull(), isNull());
    }

    // ==================== Email Stats ====================

    @Test
    @DisplayName("이메일 발송 통계를 조회한다")
    public void getEmailStats() throws Exception {
        // given
        EmailLogStatsResponse response = EmailLogStatsResponse.of(1000, 50, 5);

        // when
        Mockito.when(emailService.getEmailStats()).thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/email/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.totalSent").value(1000))
                .andExpect(jsonPath("$.payload.sentToday").value(50))
                .andExpect(jsonPath("$.payload.failedToday").value(5))
                .andExpect(jsonPath("$.payload.successRate").value(90.9));

        Mockito.verify(emailService).getEmailStats();
    }

    // ==================== Template CRUD ====================

    @Test
    @DisplayName("템플릿 목록을 조회한다")
    public void getTemplates() throws Exception {
        // given
        List<EmailTemplateResponse> templates = List.of(
                new EmailTemplateResponse(
                        1L, "환영 메일", "WELCOME", "환영합니다!", "<h1>Welcome!</h1>",
                        "HTML", LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 1, 1, 0, 0)
                ),
                new EmailTemplateResponse(
                        2L, "재방문 유도", "RETURN", "다시 만나요!", "다시 찾아주세요.",
                        "TEXT", LocalDateTime.of(2025, 1, 2, 0, 0), LocalDateTime.of(2025, 1, 2, 0, 0)
                )
        );

        // when
        Mockito.when(emailService.getTemplates()).thenReturn(templates);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/email/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(jsonPath("$.payload[0].name").value("환영 메일"))
                .andExpect(jsonPath("$.payload[0].code").value("WELCOME"))
                .andExpect(jsonPath("$.payload[1].name").value("재방문 유도"));

        Mockito.verify(emailService).getTemplates();
    }

    @Test
    @DisplayName("템플릿 단건을 조회한다")
    public void getTemplate() throws Exception {
        // given
        EmailTemplateResponse template = new EmailTemplateResponse(
                1L, "환영 메일", "WELCOME", "환영합니다!", "<h1>Welcome!</h1>",
                "HTML", LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 1, 1, 0, 0)
        );

        // when
        Mockito.when(emailService.getTemplate(1L)).thenReturn(template);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/admin/email/templates/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.id").value(1))
                .andExpect(jsonPath("$.payload.name").value("환영 메일"))
                .andExpect(jsonPath("$.payload.subject").value("환영합니다!"));

        Mockito.verify(emailService).getTemplate(1L);
    }

    @Test
    @DisplayName("템플릿을 생성한다")
    public void createTemplate() throws Exception {
        // given
        CreateEmailTemplateRequest request = new CreateEmailTemplateRequest(
                "신규 템플릿",
                "NEW_TEMPLATE",
                "신규 템플릿 제목",
                "신규 템플릿 내용입니다.",
                "TEXT"
        );
        EmailTemplateResponse response = new EmailTemplateResponse(
                3L, "신규 템플릿", "NEW_TEMPLATE", "신규 템플릿 제목", "신규 템플릿 내용입니다.",
                "TEXT", LocalDateTime.now(), LocalDateTime.now()
        );

        // when
        Mockito.when(emailService.createTemplate(any(CreateEmailTemplateRequest.class))).thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/admin/email/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.id").value(3))
                .andExpect(jsonPath("$.payload.name").value("신규 템플릿"))
                .andExpect(jsonPath("$.payload.code").value("NEW_TEMPLATE"));

        Mockito.verify(emailService).createTemplate(any(CreateEmailTemplateRequest.class));
    }

    @Test
    @DisplayName("템플릿을 수정한다")
    public void updateTemplate() throws Exception {
        // given
        UpdateEmailTemplateRequest request = new UpdateEmailTemplateRequest(
                "수정된 템플릿",
                "UPDATED_CODE",
                "수정된 제목",
                "수정된 내용입니다.",
                "HTML"
        );
        EmailTemplateResponse response = new EmailTemplateResponse(
                1L, "수정된 템플릿", "UPDATED_CODE", "수정된 제목", "수정된 내용입니다.",
                "HTML", LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.now()
        );

        // when
        Mockito.when(emailService.updateTemplate(eq(1L), any(UpdateEmailTemplateRequest.class))).thenReturn(response);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.put("/api/v1/admin/email/templates/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true))
                .andExpect(jsonPath("$.payload.name").value("수정된 템플릿"))
                .andExpect(jsonPath("$.payload.code").value("UPDATED_CODE"));

        Mockito.verify(emailService).updateTemplate(eq(1L), any(UpdateEmailTemplateRequest.class));
    }

    @Test
    @DisplayName("템플릿을 삭제한다")
    public void deleteTemplate() throws Exception {
        // given
        Mockito.doNothing().when(emailService).deleteTemplate(1L);

        // then
        mockMvc.perform(RestDocumentationRequestBuilders.delete("/api/v1/admin/email/templates/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_success").value(true));

        Mockito.verify(emailService).deleteTemplate(1L);
    }
}
