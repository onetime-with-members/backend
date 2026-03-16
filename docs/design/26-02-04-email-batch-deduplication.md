# 이메일 배치 중복 발송 방지 설계

## 개요
Spring Batch에서 마케팅 이메일 발송 시 중복 발송을 방지하기 위한 설계 문서.

## 현재 백엔드 구현 (완료)
- `email_logs` 테이블에 `user_id` 컬럼 추가
- 이메일 발송 시 `userId`를 함께 저장
- `target_group` 컬럼으로 발송 그룹 구분 (agreed, dormant, noevent, onetime, vip)

## 배치에서 구현할 내용

### 1. EmailLogRepository에 중복 체크 메서드 추가
```java
/**
 * 특정 유저에게 특정 타겟 그룹으로 이메일을 보낸 적 있는지 확인
 */
boolean existsByUserIdAndTargetGroup(Long userId, String targetGroup);

/**
 * 특정 유저에게 이메일을 보낸 적 있는지 확인 (그룹 무관)
 */
boolean existsByUserId(Long userId);
```

### 2. 배치 발송 로직 예시
```java
// 1. 발송 대상 조회 (1일 이상 이벤트 미생성 유저)
List<UserEmailDto> targets = statisticsRepository.findNoEventUserEmailsWithIds(1, 10000);

// 2. 이미 발송한 유저 제외
List<UserEmailDto> filtered = targets.stream()
    .filter(user -> !emailLogRepository.existsByUserIdAndTargetGroup(
        user.userId(), "noevent_reminder"))
    .toList();

// 3. 발송
for (UserEmailDto user : filtered) {
    sendEmail(user.email(), subject, content);
    saveEmailLog(user.userId(), user.email(), subject, "noevent_reminder");
}
```

### 3. N+1 문제 해결 (대량 발송 시)
루프에서 `existsBy...`를 호출하면 N+1 문제 발생. 대량 발송 시에는 IN 쿼리로 최적화:

```java
/**
 * 이미 발송된 userId 목록 조회 (IN 쿼리로 한 번에)
 */
@Query("SELECT el.userId FROM EmailLog el WHERE el.userId IN :userIds AND el.targetGroup = :targetGroup")
Set<Long> findSentUserIds(@Param("userIds") List<Long> userIds, @Param("targetGroup") String targetGroup);
```

```java
// 사용 예시
Set<Long> sentUserIds = emailLogRepository.findSentUserIds(
    targets.stream().map(UserEmailDto::userId).toList(),
    "noevent_reminder"
);

List<UserEmailDto> filtered = targets.stream()
    .filter(user -> !sentUserIds.contains(user.userId()))
    .toList();
```

## DDL
```sql
-- 이미 적용됨
ALTER TABLE email_logs ADD COLUMN user_id BIGINT NOT NULL AFTER id;

-- 배치 성능을 위한 인덱스 (필요 시 추가)
CREATE INDEX idx_email_logs_user_target ON email_logs(user_id, target_group);
```

## 관련 파일
- `EmailLog.java` - userId 필드 포함
- `UserEmailDto.java` - email, userId 묶음 DTO
- `StatisticsRepository.java` - XXXWithIds 쿼리들
