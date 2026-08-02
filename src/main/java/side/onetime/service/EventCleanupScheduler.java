package side.onetime.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import side.onetime.domain.Event;
import side.onetime.repository.EventRepository;
import side.onetime.infra.storage.FileStorage;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EventCleanupScheduler {

    private final EventRepository eventRepository;
    private final FileStorage fileStorage;

    /**
     * 오래된 이벤트 삭제 스케줄러.
     *
     * 매일 새벽 4시에 실행되어, 30일 이상 지난 이벤트 데이터를 삭제하고 관련된 QR 이미지를 S3에서 삭제합니다.
     * 삭제 기준은 이벤트 생성일(createdDate)이며, cron 표현식은 설정 파일에 정의됩니다.
     */
    // [25.03.20] 데이터 분석으로 인한 DB 보존 -> 스케줄러 임시 멈춤
    // @Scheduled(cron = "${scheduling.cron}")
    @Transactional
    public void deleteOldEvents() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // 30일 이상 지난 이벤트를 찾음
        List<Event> oldEvents = eventRepository.findByCreatedDateBefore(thirtyDaysAgo);

        // 이벤트 삭제 및 연관된 QR 이미지 삭제
        oldEvents.forEach(event -> {
            String qrFileName = event.getQrFileName();

            // QR 이미지 삭제
            if (qrFileName != null && !qrFileName.isEmpty()) {
                fileStorage.deleteFile(qrFileName);
            }
            // 이벤트 삭제
            eventRepository.deleteEvent(event);
        });
    }
}
