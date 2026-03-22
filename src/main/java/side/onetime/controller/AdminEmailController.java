package side.onetime.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import side.onetime.auth.annotation.IsAdmin;
import side.onetime.dto.admin.email.request.CreateEmailTemplateRequest;
import side.onetime.dto.admin.email.request.SendEmailRequest;
import side.onetime.dto.admin.email.request.SendTestEmailRequest;
import side.onetime.dto.admin.email.request.SendToGroupRequest;
import side.onetime.dto.admin.email.request.UpdateEmailTemplateRequest;
import side.onetime.dto.admin.email.response.EmailLogPageResponse;
import side.onetime.dto.admin.email.response.EmailLogStatsResponse;
import side.onetime.dto.admin.email.response.EmailTemplateResponse;
import side.onetime.dto.admin.email.response.SendEmailResponse;
import side.onetime.global.common.ApiResponse;
import side.onetime.global.common.status.SuccessStatus;
import side.onetime.service.EmailService;

/**
 * 어드민 이메일 발송 API 컨트롤러
 */
@Hidden
@RestController
@RequestMapping("/api/v1/admin/email")
@RequiredArgsConstructor
@IsAdmin
public class AdminEmailController {

    private final EmailService emailService;

    /**
     * 단일/다중 이메일 발송
     *
     * @param request 수신자 목록, 제목, 내용, 콘텐츠 타입
     * @return 발송 결과 (성공/실패 건수)
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<SendEmailResponse>> sendEmail(
            @Valid @RequestBody SendEmailRequest request) {
        SendEmailResponse response = emailService.sendEmail(request);
        return ApiResponse.onSuccess(SuccessStatus._SEND_EMAIL, response);
    }

    /**
     * 테스트 이메일 발송 (로그인된 어드민 본인에게 1통)
     *
     * @param request 제목, 내용, 콘텐츠 타입
     * @return 발송 결과
     */
    @PostMapping("/send-test")
    public ResponseEntity<ApiResponse<SendEmailResponse>> sendTestEmail(
            @Valid @RequestBody SendTestEmailRequest request) {
        SendEmailResponse response = emailService.sendTestEmail(request);
        return ApiResponse.onSuccess(SuccessStatus._SEND_TEST_EMAIL, response);
    }

    /**
     * 마케팅 타겟 그룹에 일괄 이메일 발송
     *
     * @param request 대상 그룹 (agreed, dormant, noEvent, oneTime, vip), 제목, 내용
     * @return 발송 결과 (성공/실패 건수)
     */
    @PostMapping("/send-to-group")
    public ResponseEntity<ApiResponse<SendEmailResponse>> sendToMarketingGroup(
            @Valid @RequestBody SendToGroupRequest request) {
        SendEmailResponse response = emailService.sendToMarketingGroup(request);
        return ApiResponse.onSuccess(SuccessStatus._SEND_EMAIL, response);
    }

    /**
     * 이메일 발송 로그 조회 (페이징 + 복합 필터)
     *
     * @param page        페이지 번호 (0부터 시작)
     * @param size        페이지 크기
     * @param status      상태 필터 (SENT, FAILED 등)
     * @param search      수신자/제목 검색
     * @param startDate   시작일 (yyyy-MM-dd)
     * @param endDate     종료일 (yyyy-MM-dd)
     * @param targetGroup 타겟 그룹 필터
     * @return 이메일 로그 목록
     */
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<EmailLogPageResponse>> getEmailLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String targetGroup) {
        EmailLogPageResponse response = emailService.getEmailLogs(page, size, status, search,
                startDate, endDate, targetGroup);
        return ApiResponse.onSuccess(SuccessStatus._GET_EMAIL_LOGS, response);
    }

    /**
     * 이메일 발송 통계 조회
     *
     * @return 총 발송 수, 오늘 발송 수, 오늘 실패 수, 성공률
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<EmailLogStatsResponse>> getEmailStats() {
        EmailLogStatsResponse response = emailService.getEmailStats();
        return ApiResponse.onSuccess(SuccessStatus._GET_EMAIL_STATS, response);
    }

    /**
     * 템플릿 목록 조회
     */
    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<EmailTemplateResponse>>> getTemplates() {
        List<EmailTemplateResponse> response = emailService.getTemplates();
        return ApiResponse.onSuccess(SuccessStatus._GET_EMAIL_TEMPLATES, response);
    }

    /**
     * 템플릿 단건 조회
     */
    @GetMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<EmailTemplateResponse>> getTemplate(@PathVariable Long id) {
        EmailTemplateResponse response = emailService.getTemplate(id);
        return ApiResponse.onSuccess(SuccessStatus._GET_EMAIL_TEMPLATE, response);
    }

    /**
     * 템플릿 생성
     */
    @PostMapping("/templates")
    public ResponseEntity<ApiResponse<EmailTemplateResponse>> createTemplate(
            @Valid @RequestBody CreateEmailTemplateRequest request) {
        EmailTemplateResponse response = emailService.createTemplate(request);
        return ApiResponse.onSuccess(SuccessStatus._CREATE_EMAIL_TEMPLATE, response);
    }

    /**
     * 템플릿 수정
     */
    @PutMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<EmailTemplateResponse>> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmailTemplateRequest request) {
        EmailTemplateResponse response = emailService.updateTemplate(id, request);
        return ApiResponse.onSuccess(SuccessStatus._UPDATE_EMAIL_TEMPLATE, response);
    }

    /**
     * 템플릿 삭제
     */
    @DeleteMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long id) {
        emailService.deleteTemplate(id);
        return ApiResponse.onSuccess(SuccessStatus._DELETE_EMAIL_TEMPLATE, null);
    }

}
