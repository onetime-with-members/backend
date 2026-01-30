package side.onetime.domain.enums;

/**
 * 이메일 발송 상태
 */
public enum EmailLogStatus {
    SENT,       // 발송 성공
    FAILED,     // 발송 실패
    DELIVERED,  // 전달 완료 (SES 이벤트 수신 시)
    BOUNCED,    // 반송
    COMPLAINED, // 스팸 신고
    OPENED,     // 열람
    CLICKED     // 클릭
}
