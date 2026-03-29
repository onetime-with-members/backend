package side.onetime.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import side.onetime.dto.admin.email.request.EmailEventMessage;

@Component
@RequiredArgsConstructor
public class EmailEventListener {

    private final EmailEventPublisher emailEventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEmailEvent(EmailEventMessage message) {
        emailEventPublisher.publish(message);
    }
}
