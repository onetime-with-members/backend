package side.onetime.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import side.onetime.dto.admin.email.request.SendEmailRequest;
import side.onetime.dto.admin.email.request.SendToGroupRequest;
import side.onetime.dto.admin.email.response.SendEmailResponse;
import side.onetime.global.common.ApiResponse;
import side.onetime.global.common.status.SuccessStatus;
import side.onetime.service.EmailService;

/**
 * 어드민 이메일 발송 API 컨트롤러
 */
@Hidden
@RestController
@RequestMapping("/admin/api/email")
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
}
