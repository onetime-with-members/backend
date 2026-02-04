package side.onetime.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import side.onetime.domain.EmailLog;
import side.onetime.domain.EmailTemplate;
import side.onetime.domain.enums.EmailLogStatus;
import side.onetime.dto.admin.email.request.CreateEmailTemplateRequest;
import side.onetime.dto.admin.email.request.SendByTemplateRequest;
import side.onetime.dto.admin.email.request.SendEmailRequest;
import side.onetime.dto.admin.email.request.SendToGroupRequest;
import side.onetime.dto.admin.email.request.UpdateEmailTemplateRequest;
import side.onetime.dto.admin.email.response.EmailLogPageResponse;
import side.onetime.dto.admin.email.response.EmailLogStatsResponse;
import side.onetime.dto.admin.email.response.EmailTemplateResponse;
import side.onetime.dto.admin.email.response.SendEmailResponse;
import side.onetime.dto.admin.email.response.UserEmailDto;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.EmailErrorStatus;
import side.onetime.repository.EmailLogRepository;
import side.onetime.repository.EmailTemplateRepository;
import side.onetime.repository.StatisticsRepository;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;

/**
 * 이메일 발송 서비스
 * AWS SES를 사용하여 이메일 발송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final SesClient sesClient;
    private final StatisticsRepository statisticsRepository;
    private final EmailLogRepository emailLogRepository;
    private final EmailTemplateRepository emailTemplateRepository;

    @Value("${spring.cloud.aws.ses.from-email:noreply@onetime.com}")
    private String fromEmail;

    @Value("${email.rate-limit.delay-ms:100}")
    private long rateLimitDelayMs;

    /**
     * 단일/다중 이메일 발송
     */
    @Transactional
    public SendEmailResponse sendEmail(SendEmailRequest request) {
        return sendEmailWithGroup(request, null);
    }

    /**
     * 마케팅 타겟 그룹에 일괄 발송
     */
    @Transactional
    public SendEmailResponse sendToMarketingGroup(SendToGroupRequest request) {
        List<UserEmailDto> users = getEmailsWithIdsByGroup(request.targetGroup(), request.getLimit());

        if (users.isEmpty()) {
            log.warn("[Email] 대상 그룹에 발송 가능한 이메일 없음 - 그룹: {}", request.targetGroup());
            return SendEmailResponse.success(0);
        }

        List<String> emails = users.stream().map(UserEmailDto::email).toList();
        List<Long> userIds = users.stream().map(UserEmailDto::userId).toList();

        SendEmailRequest emailRequest = new SendEmailRequest(
                emails,
                request.subject(),
                request.content(),
                request.getContentType(),
                userIds
        );

        return sendEmailWithGroup(emailRequest, request.targetGroup());
    }

    /**
     * 템플릿 코드로 이메일 발송 (배치용)
     */
    @Transactional
    public SendEmailResponse sendByTemplate(SendByTemplateRequest request) {
        EmailTemplate template = emailTemplateRepository.findByCode(request.templateCode())
                .orElseThrow(() -> new CustomException(EmailErrorStatus._EMAIL_TEMPLATE_NOT_FOUND));

        SendEmailRequest emailRequest = new SendEmailRequest(
                request.to(),
                template.getSubject(),
                template.getContent(),
                template.getContentType(),
                request.userIds()
        );

        return sendEmailWithGroup(emailRequest, request.templateCode());
    }

    /**
     * 이메일 발송 (그룹 정보 포함)
     * AWS SES Rate Limiting 적용 (기본 100ms 딜레이, 초당 10개)
     */
    private SendEmailResponse sendEmailWithGroup(SendEmailRequest request, String targetGroup) {
        List<String> failedEmails = new ArrayList<>();
        int sentCount = 0;

        List<String> recipients = request.to();
        for (int i = 0; i < recipients.size(); i++) {
            String to = recipients.get(i);
            Long userId = request.getUserIdAt(i);

            try {
                sendSingleEmail(to, request.subject(), request.content(), request.getContentType());
                sentCount++;

                saveEmailLog(userId, to, request.subject(), request.getContentType(),
                        EmailLogStatus.SENT, null, targetGroup);

            } catch (Exception e) {
                log.error("[Email] 발송 실패 - 수신자: {}, 사유: {}", to, e.getMessage());
                failedEmails.add(to);

                saveEmailLog(userId, to, request.subject(), request.getContentType(),
                        EmailLogStatus.FAILED, e.getMessage(), targetGroup);
            }

            // Rate limiting: 다음 발송 전 딜레이 적용
            if (i < recipients.size() - 1 && rateLimitDelayMs > 0) {
                applyRateLimitDelay();
            }
        }

        // 발송 결과 요약 로그
        if (failedEmails.isEmpty()) {
            log.info("[Email] 발송 완료 - 총 {}건 성공", sentCount);
        } else {
            log.warn("[Email] 발송 완료 - 성공: {}건, 실패: {}건", sentCount, failedEmails.size());
        }

        return SendEmailResponse.of(sentCount, failedEmails.size(), failedEmails);
    }

    /**
     * Rate limiting 딜레이 적용
     */
    private void applyRateLimitDelay() {
        try {
            Thread.sleep(rateLimitDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[Email] Rate Limiting 인터럽트 발생");
        }
    }

    /**
     * 이메일 로그 저장
     */
    private void saveEmailLog(Long userId, String recipient, String subject, String contentType,
                              EmailLogStatus status, String errorMessage, String targetGroup) {
        EmailLog emailLog = EmailLog.builder()
                .userId(userId)
                .recipient(recipient)
                .subject(subject)
                .contentType(contentType)
                .status(status)
                .errorMessage(errorMessage)
                .targetGroup(targetGroup)
                .build();

        emailLogRepository.save(emailLog);
    }

    /**
     * 단일 이메일 발송 (AWS SES)
     */
    private void sendSingleEmail(String to, String subject, String content, String contentType) {
        Body body;
        if ("HTML".equalsIgnoreCase(contentType)) {
            body = Body.builder()
                    .html(Content.builder().charset("UTF-8").data(content).build())
                    .build();
        } else {
            body = Body.builder()
                    .text(Content.builder().charset("UTF-8").data(content).build())
                    .build();
        }

        software.amazon.awssdk.services.ses.model.SendEmailRequest sesRequest =
                software.amazon.awssdk.services.ses.model.SendEmailRequest.builder()
                        .source(fromEmail)
                        .destination(Destination.builder().toAddresses(to).build())
                        .message(Message.builder()
                                .subject(Content.builder().charset("UTF-8").data(subject).build())
                                .body(body)
                                .build())
                        .build();

        sesClient.sendEmail(sesRequest);
    }

    /**
     * 마케팅 그룹별 이메일 + userId 목록 조회
     */
    private List<UserEmailDto> getEmailsWithIdsByGroup(String group, int limit) {
        List<Object[]> rows = switch (group.toLowerCase()) {
            case "agreed" -> statisticsRepository.findMarketingAgreedUserEmailsWithIds(limit);
            case "dormant" -> statisticsRepository.findDormantUserEmailsWithIds(30, limit);
            case "noevent" -> statisticsRepository.findNoEventUserEmailsWithIds(7, limit);
            case "onetime" -> statisticsRepository.findOneTimeUserEmailsWithIds(limit);
            case "vip" -> statisticsRepository.findVipUserEmailsWithIds(limit);
            default -> {
                log.warn("[Email] 알 수 없는 타겟 그룹 - 그룹: {}", group);
                yield List.of();
            }
        };
        return rows.stream().map(UserEmailDto::from).toList();
    }

    // ==================== 로그 조회 ====================

    /**
     * 이메일 로그 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public EmailLogPageResponse getEmailLogs(int page, int size, String status, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EmailLog> emailLogPage;

        if (search != null && !search.isBlank()) {
            emailLogPage = emailLogRepository.findByRecipientContainingOrderBySentAtDesc(search.trim(), pageable);
        } else if (status != null && !status.isBlank()) {
            try {
                EmailLogStatus emailLogStatus = EmailLogStatus.valueOf(status.toUpperCase());
                emailLogPage = emailLogRepository.findByStatusOrderBySentAtDesc(emailLogStatus, pageable);
            } catch (IllegalArgumentException e) {
                emailLogPage = emailLogRepository.findAllByOrderBySentAtDesc(pageable);
            }
        } else {
            emailLogPage = emailLogRepository.findAllByOrderBySentAtDesc(pageable);
        }

        return EmailLogPageResponse.from(emailLogPage);
    }

    /**
     * 이메일 발송 통계 조회
     */
    @Transactional(readOnly = true)
    public EmailLogStatsResponse getEmailStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long totalSent = emailLogRepository.countByStatus(EmailLogStatus.SENT);

        List<Object[]> todayStats = emailLogRepository.countByStatusSince(todayStart);
        long sentTodayCount = 0;
        long failedTodayCount = 0;

        for (Object[] stat : todayStats) {
            EmailLogStatus statusEnum = (EmailLogStatus) stat[0];
            Long count = (Long) stat[1];
            if (statusEnum == EmailLogStatus.SENT) {
                sentTodayCount = count;
            } else if (statusEnum == EmailLogStatus.FAILED) {
                failedTodayCount = count;
            }
        }

        return EmailLogStatsResponse.of(totalSent, sentTodayCount, failedTodayCount);
    }

    // ==================== 템플릿 CRUD ====================

    /**
     * 템플릿 목록 조회
     */
    @Transactional(readOnly = true)
    public List<EmailTemplateResponse> getTemplates() {
        return emailTemplateRepository.findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(EmailTemplateResponse::from)
                .toList();
    }

    /**
     * 템플릿 단건 조회
     */
    @Transactional(readOnly = true)
    public EmailTemplateResponse getTemplate(Long id) {
        EmailTemplate template = emailTemplateRepository.findById(id)
                .orElseThrow(() -> new CustomException(EmailErrorStatus._EMAIL_TEMPLATE_NOT_FOUND));
        return EmailTemplateResponse.from(template);
    }

    /**
     * 템플릿 생성
     */
    @Transactional
    public EmailTemplateResponse createTemplate(CreateEmailTemplateRequest request) {
        if (emailTemplateRepository.existsByName(request.name())) {
            throw new CustomException(EmailErrorStatus._EMAIL_TEMPLATE_NAME_DUPLICATED);
        }

        EmailTemplate template = request.toEntity();
        emailTemplateRepository.save(template);

        return EmailTemplateResponse.from(template);
    }

    /**
     * 템플릿 수정
     */
    @Transactional
    public EmailTemplateResponse updateTemplate(Long id, UpdateEmailTemplateRequest request) {
        EmailTemplate template = emailTemplateRepository.findById(id)
                .orElseThrow(() -> new CustomException(EmailErrorStatus._EMAIL_TEMPLATE_NOT_FOUND));

        if (emailTemplateRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new CustomException(EmailErrorStatus._EMAIL_TEMPLATE_NAME_DUPLICATED);
        }

        template.update(request.name(), request.code(), request.subject(), request.content(), request.contentType());

        return EmailTemplateResponse.from(template);
    }

    /**
     * 템플릿 삭제
     */
    @Transactional
    public void deleteTemplate(Long id) {
        EmailTemplate template = emailTemplateRepository.findById(id)
                .orElseThrow(() -> new CustomException(EmailErrorStatus._EMAIL_TEMPLATE_NOT_FOUND));

        emailTemplateRepository.delete(template);
    }
}
