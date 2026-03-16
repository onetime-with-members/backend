# 이벤트 확정 기능 설계 문서

## 1. 배경 및 요구사항

### 1.1 배경
OneTime은 참여자들이 가능한 시간을 투표하고, 최적의 시간을 찾아주는 서비스이다. 현재는 추천 시간을 보여주기만 하고, 이벤트를 "확정"하는 기능이 없다. 확정 기능을 추가하여 최종 일정을 결정하고 참여자에게 공유할 수 있도록 한다.

### 1.2 요구사항
- 이벤트 참여자 누구나 일정을 확정할 수 있다 (로그인 유저 + 비회원)
- 확정 시 추천 시간에서 선택하거나, 달력에서 직접 날짜/시간을 선택할 수 있다
- 확정 후에는 스케줄 수정이 불가하다
- 확정된 일정 정보를 이벤트 홈에 표시한다
- DATE 이벤트(날짜 기반), DAY 이벤트(요일 기반) 모두 지원한다

---

## 2. 데이터베이스 설계

### 2.1 신규 테이블: event_confirmations

```sql
CREATE TABLE event_confirmations (
    id                  BIGINT       PRIMARY KEY AUTO_INCREMENT,
    events_id           BIGINT       NOT NULL,
    users_id            BIGINT       NULL          COMMENT '확정한 유저 ID (비회원은 NULL)',

    -- 확정된 일정 (DATE 이벤트용: date 사용, DAY 이벤트용: day 사용)
    start_date          VARCHAR(10)  NULL          COMMENT '시작 날짜 (DATE 이벤트, 예: 2026.01.03)',
    end_date            VARCHAR(10)  NULL          COMMENT '종료 날짜 (DATE 이벤트, 예: 2026.01.05)',
    start_day           VARCHAR(10)  NULL          COMMENT '시작 요일 (DAY 이벤트, 예: 월)',
    end_day             VARCHAR(10)  NULL          COMMENT '종료 요일 (DAY 이벤트, 예: 수)',
    start_time          VARCHAR(10)  NOT NULL      COMMENT '시작 시간 (예: 18:00)',
    end_time            VARCHAR(10)  NOT NULL      COMMENT '종료 시간 (예: 20:00)',

    -- 통계/분석용 컨텍스트
    confirmer_role      VARCHAR(30)  NOT NULL      COMMENT '확정자 역할 (CREATOR, PARTICIPANT, GUEST)',
    selection_source    VARCHAR(20)  NOT NULL      COMMENT '선택 방식 (RECOMMENDED, MANUAL)',

    -- 기본 정보 (BaseEntity 제공)
    created_date        DATETIME(6)  NULL,
    updated_date        DATETIME(6)  NULL,

    INDEX idx_event_confirmations_events_id (events_id)
);
```

**설계 원칙:**
- FK 미사용 (프로젝트 컨벤션)
- VARCHAR 타입 유지 (기존 Schedule 테이블과 일관성)
- DATE/DAY 구분은 nullable 컬럼 패턴 (기존 Schedule 패턴 동일)
- `confirmed_at` 컬럼 미사용: BaseEntity의 `created_date`로 확정 시각을 대체

### 2.2 event_participations 테이블 변경

DB 컬럼명 변경 (Java 필드명과 의미 일치를 위해):

```sql
ALTER TABLE event_participations
    RENAME COLUMN event_status TO participation_role;
```

기존 인덱스도 변경:
```sql
ALTER TABLE event_participations
    DROP INDEX idx_ep_users_status,
    ADD INDEX idx_ep_users_role (users_id, participation_role);
```

### 2.3 배포 전략 (새벽 배포)

DDL 변경과 앱 배포를 동시에 진행해야 한다 (RENAME COLUMN은 메타데이터만 변경이라 즉시 완료):

```
1. DDL 적용: RENAME COLUMN event_status → participation_role
2. 즉시 앱 배포: @Column(name = "participation_role") 반영
```

---

## 3. Enum 변경사항

### 3.1 변경 요약

| Enum | 기존 | 변경 | 사용처 |
|------|------|------|--------|
| **Status** | ACTIVE, DELETED | **그대로 유지** | User, AdminUser 등 (소프트 삭제) |
| **EventStatus** | CREATOR, PARTICIPANT, CREATOR_AND_PARTICIPANT | **ACTIVE, CONFIRMED, DELETED** | Event 엔티티 전용 |
| **ParticipationRole** (신규) | - | CREATOR, PARTICIPANT, CREATOR_AND_PARTICIPANT, GUEST | EventParticipation, EventConfirmation 공용 |
| **SelectionSource** (신규) | - | RECOMMENDED, MANUAL | EventConfirmation (통계용) |

### 3.2 Event 엔티티 Enum 매핑 변경

```java
// 기존
@Enumerated(EnumType.STRING)
@Column(name = "status", nullable = false)
private Status status;

// 변경
@Enumerated(EnumType.STRING)
@Column(name = "status", nullable = false)
private EventStatus status;
```

- DB 컬럼명 `status`는 그대로 유지
- Java 타입만 `Status` → `EventStatus`로 변경
- `@SQLRestriction("status = 'ACTIVE'")` → `@SQLRestriction("status != 'DELETED'")` 변경 (CONFIRMED도 조회)

### 3.3 EventParticipation 엔티티 Enum 매핑 변경

```java
// 기존
@Enumerated(EnumType.STRING)
@Column(name = "event_status", nullable = false)
private EventStatus eventStatus;

// 변경
@Enumerated(EnumType.STRING)
@Column(name = "participation_role", nullable = false)
private ParticipationRole participationRole;
```

- DB 컬럼명 `event_status` → `participation_role` (DDL RENAME)
- Java 타입 `EventStatus` → `ParticipationRole`

---

## 4. API 설계

### 4.1 이벤트 확정 API

```
POST /api/v1/events/{event_id}/confirm
```

**Request Body:**
```json
{
  "start_date": "2026.01.03",
  "end_date": "2026.01.05",
  "start_day": null,
  "end_day": null,
  "start_time": "18:00",
  "end_time": "20:00",
  "selection_source": "RECOMMENDED"
}
```

- DATE 이벤트: `start_date`, `end_date` 사용, day는 null
- DAY 이벤트: `start_day`, `end_day` 사용, date는 null
- `selection_source`: `RECOMMENDED` (추천 시간 선택) 또는 `MANUAL` (달력 직접 선택)

**Response (200 OK):**
```json
{
  "is_success": true,
  "code": "COMMON-200",
  "message": "요청이 성공했습니다.",
  "payload": {
    "event_id": "550e8400-e29b-41d4-a716-446655440000",
    "event_status": "CONFIRMED",
    "created_date": "2026-01-03T12:00:00"
  }
}
```

**인증:**
- 로그인 유저: Authorization 헤더 (Optional)
- 비회원: 헤더 없이 호출 가능
- `@PublicApi`로 설정 (비회원도 확정 가능)

**에러 케이스:**
- EVENT-001: 이벤트를 찾을 수 없음 (404 Not Found)
- EVENT-006: 이미 확정된 이벤트 (409 Conflict)
- EVENT-007: 유효하지 않은 확정 요청 - date/day 둘 다 null, 시간 역전 등 (400 Bad Request)

### 4.2 이벤트 조회 응답 변경

**기존 GetEventResponse:**
```json
{
  "event_id": "...",
  "title": "...",
  "category": "DATE",
  "event_status": "CREATOR"
}
```

**변경 후 GetEventResponse:**
```json
{
  "event_id": "...",
  "title": "...",
  "category": "DATE",
  "event_status": "CONFIRMED",
  "participation_role": "CREATOR",
  "confirmation": {
    "start_date": "2026.01.03",
    "end_date": "2026.01.05",
    "start_time": "18:00",
    "end_time": "20:00",
    "created_date": "2026-01-03T12:00:00"
  }
}
```

**필드 변경 요약:**
- `event_status`: EventStatus (이벤트 상태: ACTIVE / CONFIRMED)
- `participation_role`: ParticipationRole (유저 역할: CREATOR / PARTICIPANT / CREATOR_AND_PARTICIPANT)
- `confirmation`: 확정 정보 (확정된 경우에만 포함, 미확정 시 @JsonInclude(NON_NULL)로 제외)
  - `created_date`: 확정 시각 (BaseEntity의 created_date 활용)

### 4.3 참여 이벤트 목록 조회 응답 변경

**GetParticipatedEventResponse** 동일하게 변경:
- `event_status` → 이벤트 상태 (EventStatus)
- `participation_role` → 유저 역할 (ParticipationRole)

---

## 5. 비즈니스 로직

### 5.1 확정 플로우

```
1. 이벤트 조회 (UUID로 검색, @Lock(PESSIMISTIC_WRITE)로 동시성 방어)
2. 이벤트 상태 검증 (ACTIVE인지 확인, CONFIRMED면 에러)
3. 요청 데이터 검증
   - category에 맞는 date/day 입력 여부
   - 날짜/요일 순서 검증 (시작 <= 종료)
   - 시간 순서 검증 (시작 < 종료)
4. 확정자 정보 결정
   - Authorization 헤더 있음 → User 조회, EventParticipation에서 역할 확인
   - Authorization 헤더 없음 → users_id = null, confirmer_role = GUEST
5. EventConfirmation 저장
6. Event.status → CONFIRMED 변경
7. 응답 반환 (created_date = BaseEntity의 created_date)
```

### 5.2 확정된 이벤트 수정 차단

확정된 이벤트에 대한 스케줄 변경 요청을 차단한다:

```
1. Event 조회
2. Event.status == CONFIRMED이면 CustomException 발생
```

**차단 대상 API (2개):**

| API | 설명 | 에러 코드 |
|-----|------|----------|
| `POST /api/v1/schedules/day` | 요일 스케줄 등록/수정 | EVENT-008 |
| `POST /api/v1/schedules/date` | 날짜 스케줄 등록/수정 | EVENT-008 |

**차단 대상 서비스 메서드:**
- `ScheduleService.createDaySchedulesForAnonymousUser()`
- `ScheduleService.createDateSchedulesForAnonymousUser()`
- `ScheduleService.createDaySchedulesForAuthenticatedUser()`
- `ScheduleService.createDateSchedulesForAuthenticatedUser()`

**차단하지 않는 API:**
- 모든 GET 요청 (조회)은 허용
- 이벤트 수정/삭제는 별도 검증 없음 (확정 여부와 무관하게 생성자만 가능)

### 5.3 confirmer_role 결정 로직

```
Authorization 헤더 없음 → "GUEST"
Authorization 헤더 있음:
  → EventParticipation 조회
  → CREATOR 또는 CREATOR_AND_PARTICIPANT → "CREATOR"
  → PARTICIPANT → "PARTICIPANT"
  → EventParticipation 없음 → "PARTICIPANT" (확정만 하고 스케줄 미등록 케이스)
```

### 5.4 이벤트 삭제/유저 탈퇴 시 확정 데이터 보존

- `EventRepositoryImpl.deleteEvent()`: 확정된 이벤트는 삭제 불가하므로, EventConfirmation 삭제 로직 불필요 (제거됨)
- `UserRepositoryImpl.withdraw()`: 참여자 누구나 확정 가능하므로, 탈퇴 시 EventConfirmation 보존

---

## 6. 엔티티 변경사항

### 6.1 신규: EventConfirmation 엔티티

```java
@Entity
@Table(name = "event_confirmations")
public class EventConfirmation extends BaseEntity {
    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "events_id", nullable = false)
    private Long eventId;

    @Column(name = "users_id")
    private Long userId;

    @Column(name = "start_date", length = 10)
    private String startDate;

    @Column(name = "end_date", length = 10)
    private String endDate;

    @Column(name = "start_day", length = 10)
    private String startDay;

    @Column(name = "end_day", length = 10)
    private String endDay;

    @Column(name = "start_time", nullable = false, length = 10)
    private String startTime;

    @Column(name = "end_time", nullable = false, length = 10)
    private String endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "confirmer_role", nullable = false, length = 30)
    private ParticipationRole confirmerRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_source", nullable = false, length = 20)
    private SelectionSource selectionSource;
}
```

- `confirmed_at` 필드 없음: BaseEntity의 `created_date`로 확정 시각 대체
- `confirmed_by` → `users_id`: 기존 네이밍 컨벤션 통일

### 6.2 Event 엔티티 수정

```java
// Enum 타입 변경
@Enumerated(EnumType.STRING)
@Column(name = "status", nullable = false)
private EventStatus status;  // Status → EventStatus

// SQLRestriction 변경
@SQLRestriction("status != 'DELETED'")  // 기존: "status = 'ACTIVE'"
```

### 6.3 EventParticipation 엔티티 수정

```java
// Enum 타입 + 필드명 + 컬럼명 변경
@Enumerated(EnumType.STRING)
@Column(name = "participation_role", nullable = false)
private ParticipationRole participationRole;
// 기존: @Column(name = "event_status") EventStatus eventStatus
```

### 6.4 영향 없는 엔티티

- **User**: Status enum 그대로 유지 (`@SQLRestriction("status = 'ACTIVE'")` 변경 없음)
- **AdminUser**: AdminStatus enum 그대로 유지
- **RefreshToken**: TokenStatus enum 그대로 유지

---

## 7. Breaking Change

### ENUM 정의

| Enum | 기존 | 변경 | 사용처 |
|------|------|------|--------|
| **Status** | ACTIVE, DELETED | **그대로 유지** | User, AdminUser 등 (소프트 삭제) |
| **EventStatus** | CREATOR, PARTICIPANT, CREATOR_AND_PARTICIPANT | **ACTIVE, CONFIRMED, DELETED** | Event 엔티티 전용 |
| **ParticipationRole** (신규) | - | CREATOR, PARTICIPANT, CREATOR_AND_PARTICIPANT, GUEST | EventParticipation, EventConfirmation 공용 |
| **SelectionSource** (신규) | - | RECOMMENDED, MANUAL | EventConfirmation (통계용) |

### 변경 대상

| 기능                           | 기존 필드명 | 변경 필드명 | 타입 | 의미 |
|------------------------------|-----------|-----------|------|------|
| 이벤트 상세 조회             | `event_status` (EventStatus) | `event_status` (EventStatus) | EventStatus | 이벤트 상태 (ACTIVE/CONFIRMED) |
| 이벤트 상세 조회             | - | `participation_role` (신규) | ParticipationRole | 유저 역할 |
| 이벤트 상세 조회             | - | `confirmation` (신규) | Object | 확정 정보 |
| 참여한 이벤트 목록 조회 | `event_status` (EventStatus) | `event_status` (EventStatus) | EventStatus | 이벤트 상태 |
| 참여한 이벤트 목록 조회 | - | `participation_role` (신규) | ParticipationRole | 유저 역할 |

### 프론트엔드 영향

- `event_status` 필드의 **의미가 변경**됨 (유저 역할 → 이벤트 상태)
  - 기존 값: `CREATOR`, `PARTICIPANT`, `CREATOR_AND_PARTICIPANT`
  - 변경 값: `ACTIVE`, `CONFIRMED`, `DELETED`
  - **🚨 필드명은 동일하지만, ENUM 값이 변경되었습니다!!**
- 기존에 `event_status`로 유저 역할을 참조하던 코드 → **`participation_role`로 변경 필요**
> **🚨기존 응답 구조가 변경되는 것이기 때문에, 이번 릴리즈는 프론트엔드와 동시 배포 필요 (새벽 중)**

---

## 8. 에러 코드

| 코드 | 메시지 | HTTP Status | 발생 조건 |
|------|--------|-------------|----------|
| EVENT-001 | 이벤트를 찾을 수 없습니다. | 404 Not Found | 존재하지 않는 event_id |
| EVENT-006 | 이미 확정된 이벤트입니다. | 409 Conflict | 이미 CONFIRMED 상태에서 재확정 시도 |
| EVENT-007 | 유효하지 않은 확정 요청입니다. | 400 Bad Request | date/day 누락, 시간 역전, 요일 순서 오류 |
| EVENT-008 | 확정된 이벤트는 수정할 수 없습니다. | 409 Conflict | CONFIRMED 이벤트의 스케줄 수정 시도 |

---

## 9. DDL 스크립트

### 9.1 신규 테이블 생성
별도 파일: `docs/sql/create-event-confirmations-table.sql`

### 9.2 컬럼 리네임
```sql
-- event_participations 컬럼명 변경
ALTER TABLE event_participations
    RENAME COLUMN event_status TO participation_role;

-- 인덱스 변경
ALTER TABLE event_participations
    DROP INDEX idx_ep_users_status,
    ADD INDEX idx_ep_users_role (users_id, participation_role);
```

### 9.3 event_confirmations Enum 컬럼 타입 변경

```sql
ALTER TABLE event_confirmations
    MODIFY confirmer_role VARCHAR(30) NOT NULL,
    MODIFY selection_source VARCHAR(20) NOT NULL;
```
