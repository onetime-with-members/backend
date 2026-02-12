package side.onetime.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import side.onetime.domain.AdminUser;
import side.onetime.domain.EmailLog;
import side.onetime.domain.EmailTemplate;
import side.onetime.domain.enums.EmailLogStatus;
import side.onetime.dto.admin.email.request.CreateEmailTemplateRequest;
import side.onetime.dto.admin.email.request.EmailEventMessage;
import side.onetime.dto.admin.email.request.SendEmailRequest;
import side.onetime.dto.admin.email.request.SendTestEmailRequest;
import side.onetime.dto.admin.email.request.SendToGroupRequest;
import side.onetime.dto.admin.email.request.UpdateEmailTemplateRequest;
import side.onetime.dto.admin.email.response.EmailLogPageResponse;
import side.onetime.dto.admin.email.response.EmailLogStatsResponse;
import side.onetime.dto.admin.email.response.EmailTemplateResponse;
import side.onetime.dto.admin.email.response.SendEmailResponse;
import side.onetime.dto.admin.email.response.UserEmailDto;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.EmailErrorStatus;
import side.onetime.global.common.status.ErrorStatus;
import side.onetime.repository.AdminRepository;
import side.onetime.repository.EmailLogRepository;
import side.onetime.repository.EmailTemplateRepository;
import side.onetime.repository.StatisticsRepository;
import side.onetime.util.AdminAuthorizationUtil;

/**
 * 이메일 서비스
 * SQS를 통한 비동기 이메일 발송 (QUEUED → 배치에서 SES 발송)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailEventPublisher emailEventPublisher;
    private final AdminRepository adminRepository;
    private final StatisticsRepository statisticsRepository;
    private final EmailLogRepository emailLogRepository;
    private final EmailTemplateRepository emailTemplateRepository;

    /**
     * 단일/다중 이메일 발송 (SQS 발행)
     */
    @Transactional
    public SendEmailResponse sendEmail(SendEmailRequest request) {
        List<EmailEventMessage.Recipient> recipients = new ArrayList<>();
        for (int i = 0; i < request.to().size(); i++) {
            String email = request.to().get(i);
            Long userId = request.getUserIdAt(i);

            EmailLog emailLog = saveQueuedEmailLog(userId, email, request.subject(),
                    request.content(), request.getContentType(), null);
            recipients.add(new EmailEventMessage.Recipient(
                    emailLog.getId(), email, userId, null, null));
        }

        EmailEventMessage message = EmailEventMessage.of(
                request.subject(), request.content(), request.getContentType(),
                null, recipients);
        emailEventPublisher.publish(message);

        log.info("[Email] SQS 발행 완료 - {}건 QUEUED", recipients.size());
        return SendEmailResponse.queued(recipients.size());
    }

    /**
     * 테스트 이메일 발송 (SQS 발행, 어드민 본인에게 1통)
     * 변수 치환은 배치 측에서 수행
     */
    @Transactional
    public SendEmailResponse sendTestEmail(SendTestEmailRequest request) {
        Long adminId = AdminAuthorizationUtil.getLoginAdminId();
        AdminUser admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorStatus._UNIDENTIFIED_USER));

        EmailLog emailLog = saveQueuedEmailLog(adminId, admin.getEmail(),
                request.subject(), request.content(), request.getContentType(), "test");

        List<EmailEventMessage.Recipient> recipients = List.of(
                new EmailEventMessage.Recipient(
                        emailLog.getId(), admin.getEmail(), adminId,
                        admin.getName(), admin.getName()));

        EmailEventMessage message = EmailEventMessage.of(
                request.subject(), request.content(), request.getContentType(),
                "test", recipients);
        emailEventPublisher.publish(message);

        log.info("[Email] 테스트 이메일 SQS 발행 완료 - 수신자: {}", admin.getEmail());
        return SendEmailResponse.queued(1);
    }

    /**
     * 마케팅 타겟 그룹에 일괄 발송 (SQS 발행)
     * 템플릿 변수는 raw 상태로 SQS에 전달, 변수 치환은 배치 측에서 수행
     */
    @Transactional
    public SendEmailResponse sendToMarketingGroup(SendToGroupRequest request) {
        List<UserEmailDto> users = getEmailsWithIdsByGroup(request.targetGroup(), request.getLimit());

        if (users.isEmpty()) {
            log.warn("[Email] 대상 그룹에 발송 가능한 이메일 없음 - 그룹: {}", request.targetGroup());
            return SendEmailResponse.empty();
        }

        List<EmailEventMessage.Recipient> recipients = new ArrayList<>();
        for (UserEmailDto user : users) {
            EmailLog emailLog = saveQueuedEmailLog(user.userId(), user.email(),
                    request.subject(), request.content(), request.getContentType(),
                    request.targetGroup());
            recipients.add(new EmailEventMessage.Recipient(
                    emailLog.getId(), user.email(), user.userId(),
                    user.name(), user.nickname()));
        }

        EmailEventMessage message = EmailEventMessage.of(
                request.subject(), request.content(), request.getContentType(),
                request.targetGroup(), recipients);
        emailEventPublisher.publish(message);

        log.info("[Email] 그룹 이메일 SQS 발행 완료 - 그룹: {}, {}건 QUEUED",
                request.targetGroup(), recipients.size());
        return SendEmailResponse.queued(recipients.size());
    }

    /**
     * QUEUED 상태 이메일 로그 저장
     */
    private EmailLog saveQueuedEmailLog(Long userId, String recipient, String subject,
                                         String content, String contentType, String targetGroup) {
        EmailLog emailLog = EmailLog.builder()
                .userId(userId)
                .recipient(recipient)
                .subject(subject)
                .content(content)
                .contentType(contentType)
                .status(EmailLogStatus.QUEUED)
                .targetGroup(targetGroup)
                .build();
        return emailLogRepository.save(emailLog);
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
     * 이메일 로그 목록 조회 (페이징 + 복합 필터)
     */
    @Transactional(readOnly = true)
    public EmailLogPageResponse getEmailLogs(int page, int size, String status, String search,
                                              String startDateStr, String endDateStr, String targetGroup) {
        Pageable pageable = PageRequest.of(page, size);

        LocalDateTime startDate = startDateStr != null && !startDateStr.isBlank()
                ? LocalDate.parse(startDateStr).atStartOfDay() : null;
        LocalDateTime endDate = endDateStr != null && !endDateStr.isBlank()
                ? LocalDate.parse(endDateStr).plusDays(1).atStartOfDay() : null;

        List<EmailLog> logs = emailLogRepository.findAllWithFilters(pageable, search, startDate, endDate,
                status, targetGroup);
        long totalElements = emailLogRepository.countWithFilters(search, startDate, endDate, status, targetGroup);

        return EmailLogPageResponse.of(logs, page, size, totalElements);
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
