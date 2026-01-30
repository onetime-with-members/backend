package side.onetime.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import side.onetime.dto.admin.email.request.SendEmailRequest;
import side.onetime.dto.admin.email.request.SendToGroupRequest;
import side.onetime.dto.admin.email.response.EmailLogPageResponse;
import side.onetime.dto.admin.email.response.EmailLogStatsResponse;
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
     * 이메일 발송 로그 조회 (페이징)
     *
     * @param page   페이지 번호 (0부터 시작)
     * @param size   페이지 크기
     * @param status 상태 필터 (SENT, FAILED 등)
     * @param search 수신자 검색
     * @return 이메일 로그 목록
     */
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<EmailLogPageResponse>> getEmailLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        EmailLogPageResponse response = emailService.getEmailLogs(page, size, status, search);
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
}
