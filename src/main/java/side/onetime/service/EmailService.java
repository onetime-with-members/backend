package side.onetime.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import side.onetime.domain.enums.EmailLogStatus;
import side.onetime.dto.admin.email.request.SendEmailRequest;
import side.onetime.dto.admin.email.request.SendToGroupRequest;
import side.onetime.dto.admin.email.response.EmailLogPageResponse;
import side.onetime.dto.admin.email.response.EmailLogStatsResponse;
import side.onetime.dto.admin.email.response.SendEmailResponse;
import side.onetime.repository.EmailLogRepository;
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

    @Value("${spring.cloud.aws.ses.from-email:noreply@onetime.com}")
    private String fromEmail;

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
        List<String> emails = getEmailsByGroup(request.targetGroup(), request.getLimit());

        if (emails.isEmpty()) {
            log.warn("No emails found for target group: {}", request.targetGroup());
            return SendEmailResponse.success(0);
        }

        log.info("Sending emails to {} users in group: {}", emails.size(), request.targetGroup());

        SendEmailRequest emailRequest = new SendEmailRequest(
                emails,
                request.subject(),
                request.content(),
                request.getContentType()
        );

        return sendEmailWithGroup(emailRequest, request.targetGroup());
    }

    /**
     * 이메일 발송 (그룹 정보 포함)
     */
    private SendEmailResponse sendEmailWithGroup(SendEmailRequest request, String targetGroup) {
        List<String> failedEmails = new ArrayList<>();
        int sentCount = 0;

        for (String to : request.to()) {
            try {
                sendSingleEmail(to, request.subject(), request.content(), request.getContentType());
                sentCount++;
                log.info("Email sent successfully to: {}", to);

                // 성공 로그 저장
                saveEmailLog(to, request.subject(), request.getContentType(),
                        EmailLogStatus.SENT, null, targetGroup);

            } catch (Exception e) {
                log.error("Failed to send email to: {}, error: {}", to, e.getMessage());
                failedEmails.add(to);

                // 실패 로그 저장
                saveEmailLog(to, request.subject(), request.getContentType(),
                        EmailLogStatus.FAILED, e.getMessage(), targetGroup);
            }
        }

        return SendEmailResponse.of(sentCount, failedEmails.size(), failedEmails);
    }

    /**
     * 이메일 로그 저장
     */
    private void saveEmailLog(String recipient, String subject, String contentType,
                              EmailLogStatus status, String errorMessage, String targetGroup) {
        EmailLog emailLog = EmailLog.builder()
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
     * 마케팅 그룹별 이메일 목록 조회
     */
    private List<String> getEmailsByGroup(String group, int limit) {
        return switch (group.toLowerCase()) {
            case "agreed" -> statisticsRepository.findMarketingAgreedUserEmails(limit);
            case "dormant" -> statisticsRepository.findDormantUserEmails(30, limit);
            case "noevent" -> statisticsRepository.findNoEventUserEmails(7, limit);
            case "onetime" -> statisticsRepository.findOneTimeUserEmails(limit);
            case "vip" -> statisticsRepository.findVipUserEmails(limit);
            default -> {
                log.warn("Unknown target group: {}", group);
                yield List.of();
            }
        };
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
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        long totalSent = emailLogRepository.countByStatus(EmailLogStatus.SENT);
        long sentToday = emailLogRepository.count(); // 임시: 오늘 발송 건수 계산

        // 오늘 통계 계산
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
}
