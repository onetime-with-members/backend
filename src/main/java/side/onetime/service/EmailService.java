package side.onetime.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import side.onetime.dto.admin.email.request.SendEmailRequest;
import side.onetime.dto.admin.email.request.SendToGroupRequest;
import side.onetime.dto.admin.email.response.SendEmailResponse;
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

    @Value("${spring.cloud.aws.ses.from-email:noreply@onetime.com}")
    private String fromEmail;

    /**
     * 단일/다중 이메일 발송
     */
    public SendEmailResponse sendEmail(SendEmailRequest request) {
        List<String> failedEmails = new ArrayList<>();
        int sentCount = 0;

        for (String to : request.to()) {
            try {
                sendSingleEmail(to, request.subject(), request.content(), request.getContentType());
                sentCount++;
                log.info("Email sent successfully to: {}", to);
            } catch (Exception e) {
                log.error("Failed to send email to: {}, error: {}", to, e.getMessage());
                failedEmails.add(to);
            }
        }

        return SendEmailResponse.of(sentCount, failedEmails.size(), failedEmails);
    }

    /**
     * 마케팅 타겟 그룹에 일괄 발송
     */
    @Transactional(readOnly = true)
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

        return sendEmail(emailRequest);
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
}
