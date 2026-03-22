package side.onetime.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import side.onetime.dto.admin.email.request.EmailEventMessage;
import side.onetime.exception.CustomException;
import side.onetime.exception.status.EmailErrorStatus;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventPublisher {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${spring.cloud.aws.sqs.queue-url}")
    private String queueUrl;

    public void publish(EmailEventMessage message) {
        try {
            String messageBody = objectMapper.writeValueAsString(message);

            log.info("[EmailEventPublisher] SQS 발행 - 수신자: {}명, 그룹: {}",
                    message.recipients().size(), message.targetGroup());

            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build());

            log.info("[EmailEventPublisher] SQS 발행 완료");
        } catch (JsonProcessingException e) {
            log.error("[EmailEventPublisher] 메시지 직렬화 실패", e);
            throw new CustomException(EmailErrorStatus._EMAIL_SQS_PUBLISH_FAILED);
        } catch (Exception e) {
            log.error("[EmailEventPublisher] SQS 발행 실패", e);
            throw new CustomException(EmailErrorStatus._EMAIL_SQS_PUBLISH_FAILED);
        }
    }
}
