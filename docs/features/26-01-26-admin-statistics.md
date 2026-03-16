# 어드민 통계 대시보드

## 개요

어드민 전용 통계 대시보드로, 유저/이벤트 현황, 리텐션 분석, 마케팅 타겟 관리, 이메일 발송 기능을 제공합니다.

- **브랜치**: `feature/#325/statistics`
- **이슈**: #325
- **접근 경로**: `/admin/*`
- **커밋 수**: 70+

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| 백엔드 | Spring Boot 3.3, Spring Security, QueryDSL |
| 프론트엔드 | Thymeleaf, **Tailwind CSS** (다크 모드), Chart.js, Lucide Icons |
| 이메일 | AWS SQS (비동기 큐) → Batch → AWS SES (발송) |
| 인증 | JWT (Access + Refresh Token), HttpOnly 쿠키 |
| 보안 | `@IsAdmin`, `@IsMasterAdmin`, `@IsUser`, `@PublicApi` 커스텀 어노테이션 |

> ⚠️ SB Admin 2 (Bootstrap 4)에서 **Tailwind CSS로 마이그레이션** 완료
> ⚠️ 다국어(i18n) 기능 **제거됨** (한국어 단일 언어)
> ⚠️ 이메일 발송이 **SES 동기 → SQS 비동기 큐 방식으로 전환**됨 (Batch가 소비)

---

## 디렉토리 구조

```
src/main/java/side/onetime/
├── controller/
│   ├── AdminPageController.java          # 페이지 라우팅 (/admin/*)
│   ├── AdminController.java              # 인증 API (/api/v1/admin/*)
│   ├── AdminStatisticsController.java    # 통계 API (/api/v1/admin/statistics/*)
│   ├── AdminEmailController.java         # 이메일 API (/api/v1/admin/email/*)
│   ├── AdminExportController.java        # CSV 내보내기 (/api/v1/admin/export/*)
│   └── AdminErrorController.java         # 404 에러 페이지
├── service/
│   ├── StatisticsService.java            # 통계 비즈니스 로직
│   ├── EmailService.java                 # 이메일 발송 + 템플릿 관리
│   └── EmailEventPublisher.java          # SQS 메시지 발행
├── repository/
│   ├── StatisticsRepository.java         # Native Query 통계
│   ├── EmailLogRepository.java           # 이메일 발송 로그
│   ├── EmailTemplateRepository.java      # 이메일 템플릿
│   ├── BannerStagingRepository.java      # 배너 스테이징 (내보내기/불러오기)
│   └── BarBannerStagingRepository.java   # 띠배너 스테이징
├── domain/
│   ├── EmailLog.java                     # 이메일 발송 로그 엔티티
│   ├── EmailTemplate.java                # 이메일 템플릿 엔티티
│   ├── BannerStaging.java                # 배너 스테이징 엔티티
│   ├── BarBannerStaging.java             # 띠배너 스테이징 엔티티
│   └── enums/EmailLogStatus.java         # QUEUED, SENT, FAILED, DELIVERED, BOUNCED, ...
├── dto/admin/
│   ├── statistics/response/              # 통계 응답 DTO
│   └── email/
│       ├── request/                      # 이메일 요청 DTO + EmailEventMessage (SQS DTO)
│       └── response/                     # 이메일 응답 DTO
├── auth/annotation/
│   ├── IsAdmin.java                      # @PreAuthorize("hasRole('ADMIN')")
│   ├── IsMasterAdmin.java                # @PreAuthorize("hasRole('MASTER_ADMIN')")
│   ├── IsUser.java                       # @PreAuthorize("hasRole('USER')")
│   └── PublicApi.java                    # 인증 불필요 API
├── global/config/
│   └── SqsConfig.java                   # AWS SQS 클라이언트 빈
├── exception/status/
│   ├── AdminErrorStatus.java             # 어드민 에러 코드
│   └── EmailErrorStatus.java             # 이메일 에러 코드
└── util/
    ├── DateUtil.java                     # 날짜 범위 처리
    └── CookieUtil.java                   # 쿠키 유틸리티 (Secure, SameSite)

src/main/resources/templates/admin/
├── layout/default.html                   # 공통 레이아웃 (Tailwind)
├── fragments/
│   ├── header.html                       # <head> 공통 (CSS, JS)
│   ├── sidebar.html                      # 사이드바
│   ├── topbar.html                       # 탑바 (날짜 필터)
│   ├── date-filter.html                  # 날짜 필터 컴포넌트
│   ├── modal.html                        # 커스텀 모달
│   └── command-palette.html              # 커맨드 팔레트 (Cmd+K)
├── dashboard.html                        # 메인 대시보드
├── users.html                            # 유저 통계
├── events.html                           # 이벤트 통계
├── retention.html                        # 리텐션 분석
├── marketing.html                        # 마케팅 타겟
├── email.html                            # 이메일 발송 + 템플릿 관리
├── login.html                            # 로그인
└── 404.html                              # 404 에러 페이지
```

---

## 페이지 목록

| 경로 | 페이지 | 설명 |
|------|--------|------|
| `/admin/login` | 로그인 | 어드민 계정 로그인 |
| `/admin/dashboard` | 대시보드 | KPI 카드, 월별 가입, OAuth 분포, 요일별 이벤트, 키워드 TOP |
| `/admin/statistics/users` | 유저 통계 | 유저 목록, 검색/필터, 상세 모달, CSV 내보내기 |
| `/admin/statistics/events` | 이벤트 통계 | 이벤트 목록, 히트맵, 키워드 검색, CSV 내보내기 |
| `/admin/statistics/retention` | 리텐션 | MAU, 퍼널, TTV, Stickiness, 코호트, 유저 모달 |
| `/admin/statistics/marketing` | 마케팅 | 타겟 그룹별 유저 목록, 체크박스 선택, 이메일 발송 |
| `/admin/email` | 이메일 | 그룹/개별 발송, 템플릿 CRUD, 발송 로그 |
| `/admin/logout` | 로그아웃 | 쿠키 삭제 후 로그인 페이지로 |

---

## API 엔드포인트

> 모든 통계/이메일 API는 Swagger에서 숨김 처리됨 (`@Hidden`)

### 인증 (AdminController)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/v1/admin/action-login` | 로그인 (JWT 발급) |
| POST | `/api/v1/admin/action-reissue` | 토큰 재발급 |

### 유저/이벤트 목록 (AdminStatisticsController)

| Method | Endpoint | 파라미터 | 설명 |
|--------|----------|----------|------|
| GET | `/api/v1/admin/statistics/users` | page, size, keyword, sorting, search, startDate, endDate | 유저 목록 (페이징) |
| GET | `/api/v1/admin/statistics/users/{userId}/detail` | - | 유저 상세 정보 |
| GET | `/api/v1/admin/statistics/events` | page, size, keyword, sorting, search, startDate, endDate, hour, dayOfWeek | 이벤트 목록 (페이징) |

### 리텐션 분석 (AdminStatisticsController)

| Method | Endpoint | 파라미터 | 설명 |
|--------|----------|----------|------|
| GET | `/api/v1/admin/statistics/funnel` | startDate, endDate | 전환 퍼널 (가입→이벤트→참여자→재생성) |
| GET | `/api/v1/admin/statistics/cohort` | months (기본 12) | 코호트 리텐션 테이블 |
| GET | `/api/v1/admin/statistics/ttv` | startDate, endDate | TTV 분포 (Time to Value) |
| GET | `/api/v1/admin/statistics/stickiness` | months (기본 12) | Stickiness (WAU/MAU) 추이 |
| GET | `/api/v1/admin/statistics/heatmap` | startDate, endDate | 시간대×요일 히트맵 |
| GET | `/api/v1/admin/statistics/retention/dormant` | days, startDate, endDate | 휴면 유저 목록 |
| GET | `/api/v1/admin/statistics/retention/returning` | startDate, endDate | 복귀 유저 목록 (이벤트 2회+) |

### 이벤트 확정 통계 (AdminStatisticsController)

| Method | Endpoint | 파라미터 | 설명 |
|--------|----------|----------|------|
| GET | `/api/v1/admin/statistics/events/confirmation` | startDate, endDate | 확정률, 확정자 유형, 카테고리별 확정률, 일별 추이 |

### 마케팅 타겟 (AdminStatisticsController)

| Method | Endpoint | 파라미터 | 설명 |
|--------|----------|----------|------|
| GET | `/api/v1/admin/statistics/marketing/agreed` | startDate, endDate | 마케팅 동의 유저 |
| GET | `/api/v1/admin/statistics/marketing/dormant` | startDate, endDate | 휴면 유저 (30일 미접속) |
| GET | `/api/v1/admin/statistics/marketing/no-event` | startDate, endDate | 이벤트 미생성 유저 |
| GET | `/api/v1/admin/statistics/marketing/one-time` | startDate, endDate | 1회성 유저 |
| GET | `/api/v1/admin/statistics/marketing/vip` | startDate, endDate | VIP 유저 (이벤트 5개+) |
| GET | `/api/v1/admin/statistics/marketing/zero-participant` | startDate, endDate | 참여자 0명 이벤트 |

> 모든 마케팅 API에 **날짜 범위 필터** 적용, **limit 제한 없음** (전체 조회 가능)

### 이메일 발송 (AdminEmailController)

| Method | Endpoint | Body | 설명 |
|--------|----------|------|------|
| POST | `/api/v1/admin/email/send` | `SendEmailRequest` | 개별 이메일 발송 |
| POST | `/api/v1/admin/email/send-to-group` | `SendToGroupRequest` | 그룹 이메일 발송 |
| POST | `/api/v1/admin/email/send-by-template` | `SendByTemplateRequest` | **템플릿 코드로 발송 (배치용)** |
| GET | `/api/v1/admin/email/logs` | page, size, status, search | 발송 로그 조회 |
| GET | `/api/v1/admin/email/stats` | - | 발송 통계 |

### 이메일 템플릿 (AdminEmailController)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/v1/admin/email/templates` | 템플릿 목록 |
| GET | `/api/v1/admin/email/templates/{id}` | 템플릿 상세 |
| POST | `/api/v1/admin/email/templates` | 템플릿 생성 |
| PUT | `/api/v1/admin/email/templates/{id}` | 템플릿 수정 |
| DELETE | `/api/v1/admin/email/templates/{id}` | 템플릿 삭제 |

### CSV 내보내기 (AdminExportController)

| Method | Endpoint | 파라미터 | 설명 |
|--------|----------|----------|------|
| GET | `/api/v1/admin/export/users/csv` | keyword, sorting, search, startDate, endDate | 유저 CSV |
| GET | `/api/v1/admin/export/events/csv` | keyword, sorting, search, startDate, endDate | 이벤트 CSV |
| GET | `/api/v1/admin/export/marketing/csv` | group, startDate, endDate | 마케팅 타겟 CSV |

---

## 데이터베이스 스키마

### email_logs

이메일 발송 로그를 저장합니다.

```sql
CREATE TABLE email_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '발송 대상 유저 ID',
    recipient VARCHAR(255) NOT NULL COMMENT '수신자 이메일',
    subject VARCHAR(500) NOT NULL COMMENT '이메일 제목',
    content_type VARCHAR(10) COMMENT 'TEXT 또는 HTML',
    status VARCHAR(20) NOT NULL COMMENT 'SENT 또는 FAILED',
    error_message TEXT COMMENT '실패 시 에러 메시지',
    target_group VARCHAR(50) COMMENT '타겟 그룹 (agreed, dormant 등)',
    sent_at DATETIME NOT NULL COMMENT '발송 시간'
);

CREATE INDEX idx_email_logs_status ON email_logs(status);
CREATE INDEX idx_email_logs_recipient ON email_logs(recipient);
CREATE INDEX idx_email_logs_sent_at ON email_logs(sent_at);
CREATE INDEX idx_email_logs_user_id ON email_logs(user_id);
```

### email_templates

이메일 템플릿을 저장합니다.

```sql
CREATE TABLE email_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '템플릿 이름 (UI 표시용)',
    code VARCHAR(50) UNIQUE COMMENT '템플릿 코드 (배치 연동용)',
    subject VARCHAR(500) NOT NULL COMMENT '이메일 제목',
    content TEXT NOT NULL COMMENT '이메일 본문',
    content_type VARCHAR(10) NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT 또는 HTML',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE UNIQUE INDEX idx_email_templates_code ON email_templates(code);
```

---

## DTO 구조

### 이메일 요청 DTO

#### SendEmailRequest
```java
public record SendEmailRequest(
    @NotEmpty List<String> to,        // 수신자 이메일 목록
    @NotBlank String subject,          // 제목
    @NotBlank String content,          // 내용
    String contentType,                // TEXT 또는 HTML (기본: TEXT)
    @NotEmpty List<Long> userIds       // 수신자 userId 목록 (to와 1:1 매칭)
) {
    // to.size() != userIds.size() 일 경우 IllegalArgumentException
}
```

#### SendToGroupRequest
```java
public record SendToGroupRequest(
    @NotBlank String targetGroup,      // agreed, dormant, noevent, onetime, vip
    @NotBlank String subject,
    @NotBlank String content,
    String contentType,
    Integer limit                      // 선택적 제한 (기본: 제한 없음)
)
```

#### SendByTemplateRequest
```java
public record SendByTemplateRequest(
    @NotBlank String templateCode,     // 템플릿 코드 (배치에서 사용)
    @NotEmpty List<String> to,
    List<Long> userIds
)
```

#### CreateEmailTemplateRequest
```java
public record CreateEmailTemplateRequest(
    @NotBlank String name,
    String code,                       // 선택적 (배치 연동 시 필요)
    @NotBlank String subject,
    @NotBlank String content,
    String contentType
)
```

---

## 에러 코드

### AdminErrorStatus

| 코드 | HTTP Status | 메시지 |
|------|-------------|--------|
| ADMIN-USER-001 | 400 | 이미 존재하는 이메일입니다. |
| ADMIN-USER-002 | 404 | 관리자 계정을 찾을 수 없습니다. |
| ADMIN-USER-003 | 401 | 승인되지 않은 관리자 계정입니다. |
| ADMIN-USER-004 | 400 | 등록된 비밀번호와 다릅니다. |
| ADMIN-USER-005 | 401 | 마스터 관리자만 사용 가능한 기능입니다. |
| ADMIN-USER-006 | 400 | 지원하지 않는 정렬 기준입니다. |

### EmailErrorStatus

| 코드 | HTTP Status | 메시지 |
|------|-------------|--------|
| EMAIL-001 | 404 | 이메일 템플릿을 찾을 수 없습니다. |
| EMAIL-002 | 400 | 이미 존재하는 템플릿 이름입니다. |

---

## 보안

### JWT 인증

- **Access Token**: 30분 유효
- **Refresh Token**: 7일 유효
- **저장 방식**: HttpOnly 쿠키

```
Cookie: admin_access_token=eyJ...
Cookie: admin_refresh_token=eyJ...
```

### 쿠키 보안 설정 (CookieUtil)

| 속성 | 값 | 설명 |
|------|-----|------|
| HttpOnly | true | JavaScript 접근 차단 (XSS 방지) |
| Secure | 항상 설정 | HTTPS에서만 전송 (localhost 예외) |
| SameSite | Lax | CSRF 방지 (같은 사이트 + 링크 클릭 허용) |
| Path | / | 전체 경로에서 사용 |

### 토큰 자동 재발급

Access Token 만료 시 JwtFilter에서 자동 재발급:
1. Access Token 검증 실패
2. Refresh Token으로 재발급 요청
3. 새 토큰을 쿠키에 설정
4. 요청 계속 처리

### 이메일 비동기 발송 (SQS)

이메일 발송은 SQS 비동기 큐 방식으로 처리됩니다:
1. Backend: `EmailService` → `EmailEventPublisher.publish()` → SQS 큐 발행 (상태: QUEUED)
2. Batch: `EmailEventListener` (@SqsListener) → SES 발송 → email_logs 상태 업데이트 (SENT/FAILED)

Rate Limiting은 Batch 서버에서 SES 발송 시 처리 (`aws.ses.rate-limit-delay-ms: 100`)

---

## 주요 기능 상세

### 1. 대시보드

**KPI 카드 (7개)**
| 카드 | 데이터 | 링크 |
|------|--------|------|
| 총 가입 유저 | 선택 기간 내 가입자 수 + 전기간 대비 % | /admin/statistics/users |
| 총 생성 이벤트 | 선택 기간 내 이벤트 수 + 전기간 대비 % | /admin/statistics/events |
| MAU (최근 30일) | 최근 30일 로그인 유저 + 전기간 대비 % | /admin/statistics/retention |
| 휴면율 (60일+) | 60일+ 미접속 비율 + 전기간 대비 p | /admin/statistics/retention |
| 평균 참여자/이벤트 | 이벤트당 평균 참여 인원 | /admin/statistics/events |
| 이벤트 확정률 | 확정/전체 이벤트 비율 | /admin/statistics/events |
| 마케팅 타겟 | 마케팅 수신 동의 유저 수 | /admin/statistics/marketing |

**차트**
- 월별 신규 가입 (Line Chart, 이전 기간 비교 점선)
- OAuth 제공자 비율 (Doughnut Chart)
- 요일별 이벤트 분포 (Bar Chart)
- 이벤트 제목 키워드 TOP 10 (테이블, 클릭 시 검색)

### 2. 유저 통계

**Summary Cards (3개)**
- 총 가입 유저
- 활성 유저 (기간 내 로그인)
- 마케팅 동의율

**분포 테이블**
- OAuth 제공자 분포 (KAKAO, GOOGLE, NAVER)
- 언어 분포 (KO, EN 등)

**유저 목록**
- 페이징 (20개씩)
- 검색 (이름, 이메일, 닉네임)
- 정렬 (최신순, 오래된순, 이름순)
- 클릭 시 상세 모달 (활동 정보 포함)
- CSV 내보내기

### 3. 이벤트 통계

**Summary Cards (5개)**
- 총 생성 이벤트
- 활성 이벤트
- 확정 이벤트 (CONFIRMED 상태)
- 확정률 (확정/전체 비율)
- 평균 확정 소요 (생성→확정 시간)

**분포 테이블**
- 카테고리 분포 (DATE, DAY)
- 요일별 분포

**확정 분석**
- 확정자 유형 분포 (Doughnut Chart: CREATOR, PARTICIPANT, GUEST)
- 카테고리별 확정률 (Horizontal Bar Chart: DATE vs DAY)
- 확정 추이 (Line Chart: 일별 생성 vs 확정 건수)

**히트맵**
- 시간대(0-23) × 요일(월-일) 매트릭스
- 셀 클릭 시 해당 조건으로 이벤트 목록 필터링

**이벤트 목록**
- 페이징 (20개씩)
- 검색 (제목)
- 정렬 (최신순, 참여자순)
- **확정 상태 배지** (확정/미확정)
- 히트맵 필터 적용
- CSV 내보내기

### 4. 리텐션 분석

**Summary Cards (4개, 클릭 시 유저 목록 모달)**
- 휴면 7일+
- 휴면 30일+
- 휴면 90일+ (이탈 위험)
- 복귀율 (이벤트 2회+ 참여)

**MAU 차트**
- 월별 활성 사용자 추이 (Line Chart)

**전환 퍼널**
```
가입 (100%)
  ↓
첫 이벤트 생성 (X%, -Y% 이탈)
  ↓
참여자 1명+ 확보 (X%, -Y% 이탈)
  ↓
2번째 이벤트 생성 (X%, -Y% 이탈)
```

**TTV (Time to Value)**
- 평균/중앙값 (0일이면 '당일' 표시)
- 활성화율 (이벤트 생성 유저 / 전체)
- 분포 차트 (당일, 1-3일, 4-7일, 8-14일, 15-30일, 31일+)

**Stickiness**
- 현재 Stickiness (WAU/MAU %)
- WAU (최근 7일 활성)
- MAU (최근 30일 활성)
- 월별 추이 차트

**코호트 리텐션**
- 가입월별 N개월 후 재방문율
- 히트맵 시각화 (색상 강도 = 리텐션율)
- 6개월/12개월 선택

### 5. 마케팅 타겟

**Summary Cards (6개, 클릭 시 유저 목록 표시)**
| 그룹 | 설명 |
|------|------|
| agreed | 마케팅 동의 유저 |
| dormant | 휴면 유저 (30일+ 미접속) |
| noevent | 이벤트 미생성 유저 (가입 7일+ 경과) |
| onetime | 일회성 유저 (이벤트 1개만) |
| vip | VIP 유저 (이벤트 5개+) |
| zeroParticipant | 참여자 0명 이벤트 |

**기능**
- 날짜 범위 필터 적용
- 체크박스로 유저 선택
- 선택된 유저에게 이메일 발송 (이메일 페이지로 이동)

### 6. 이메일 시스템

**발송 탭**
- 수신자 직접 입력 또는 마케팅 그룹 선택
- 제목/내용 입력
- TEXT/HTML 선택
- 발송 결과 표시 (성공/실패 건수)

**템플릿 탭**
- 템플릿 목록 (이름, 코드, 제목, 수정일)
- 템플릿 생성/수정/삭제
- 템플릿 선택 시 내용 자동 입력

**로그 탭**
- 발송 로그 목록 (수신자, 제목, 상태, 발송 시간)
- 상태별 필터 (SENT, FAILED)
- 검색 (수신자)
- 통계 (총 발송, 오늘 발송, 오늘 실패)

---

## UI/UX

### 다크 모드
- Tailwind CSS `dark:` 클래스 사용
- localStorage 기반 테마 저장
- Chart.js 테마 연동 (그리드, 텍스트, 툴팁 색상)
- 시스템 설정 자동 감지

### 공통 CSS 클래스

| 클래스 | 효과 |
|--------|------|
| `tabular-nums` | 숫자 고정폭 (font-variant-numeric: tabular-nums) |
| `card-hover` | 카드 hover 시 translateY(-2px) + box-shadow |
| `btn-hover` | 버튼 hover 시 translateY(-1px) + box-shadow |
| `toast-enter` | 토스트 slide-in 애니메이션 (오른쪽에서 진입) |
| `toast-exit` | 토스트 slide-out 애니메이션 (오른쪽으로 퇴장) |

### 레이아웃
- 반응형 사이드바
- 탑바 (유저 이메일 첫 글자 아바타, 날짜 필터)
- 커맨드 팔레트 (Cmd+K / Ctrl+K)

### 컴포넌트
- **커스텀 모달** (`showAlert()`, `showConfirm()` - modal.html)
- **스켈레톤 로딩** (리스트, 테이블, 차트)
- **토스트 알림** (`showToast()` - header.html, slide-in/out 애니메이션)
- **뱃지** (상태 표시, 라이트/다크 모드 대응)

#### 알림 시스템 사용법
```javascript
// 토스트 (가벼운 알림, 자동 소멸)
showToast('저장되었습니다', 'success');  // success, error, warning, info
showToast('에러가 발생했습니다', 'error', 3000);  // duration 지정

// 모달 (중요한 알림, 사용자 확인 필요)
showAlert('작업이 완료되었습니다', 'success');
const confirmed = await showConfirm('삭제하시겠습니까?');
```

### 날짜 필터
- 글로벌 날짜 범위 필터 (탑바)
- 빠른 선택: 7일, 30일, 90일, 1년
- **로컬 시간대 기준** (UTC+9)
- URL 파라미터로 상태 유지

---

## 설정

### application.yaml

```yaml
# AWS SQS (이메일 비동기 큐)
spring:
  cloud:
    aws:
      region:
        static: ap-northeast-2
      sqs:
        queue-url: ${AWS_SQS_EMAIL_QUEUE_URL:}
```

### 환경변수

| 변수 | 필수 | 설명 |
|------|------|------|
| `ADMIN_PASSWORD` | Y | 어드민 계정 비밀번호 |
| `S3_ACCESS_KEY` | Y | AWS 인증 (S3, SQS 공용) |
| `S3_SECRET_KEY` | Y | AWS 인증 (S3, SQS 공용) |
| `AWS_SQS_EMAIL_QUEUE_URL` | Y | SQS 이메일 큐 URL |

---

## 로깅 컨벤션

어드민/이메일 도메인 로그 형식:

```
[Domain] 메시지 - key: value, key2: value2
```

**예시:**
```java
log.warn("[Admin] 로그인 실패 - 사유: {}", e.getMessage());
log.warn("[Email] 발송 실패 - 수신자: {}, 사유: {}", to, e.getMessage());
log.info("[Email] 발송 완료 - 총 {}건 성공", sentCount);
```

**규칙:**
- info: 배치 작업 요약 (발송 완료 등)
- warn/error: 예외 상황 (로그인 실패, 발송 실패 등)
- debug: 개발용 상세 로그

---

## 테스트 명령어

```bash
# 전체 빌드 + 테스트
./gradlew clean build

# 테스트만
./gradlew test

# 로컬 실행
./gradlew bootRun --args='--spring.profiles.active=local'
```

---

## 향후 개선 사항

### 완료됨 ✅
- [x] Cookie Secure 플래그 항상 설정
- [x] Admin BCrypt 비밀번호 해싱
- [x] XSS 방지 (escapeHtml, ARIA 접근성)
- [x] CSV Formula Injection 방지
- [x] 이벤트 확정 통계 (확정률, 확정자 유형, 카테고리별, 추이)
- [x] 반응형 디자인 (모바일 사이드바, 카드/테이블/폼)
- [x] Safari PWA 지원
- [x] Logout GET → POST
- [x] 이메일 Rate Limiting
- [x] tabular-nums 숫자 정렬
- [x] card-hover, btn-hover 마이크로 인터랙션
- [x] 로그 한국어화 및 컨벤션 적용
- [x] Javadoc 주석 추가
- [x] 토스트 애니메이션 개선 (slide-in/out)
- [x] 차트 툴팁 스타일 통일 (ChartTooltipConfig)
- [x] 로딩 상태 피드백 강화 (skeleton loaders)
- [x] SES 동기 → SQS 비동기 큐 이메일 전환
- [x] 커스텀 보안 어노테이션 (@IsAdmin, @IsUser, @PublicApi) + SecurityAnnotationTest
- [x] 배너 스테이징 (내보내기/불러오기) 기능
- [x] 사이드/탑 바 스티키 레이아웃

### 향후 고려
- [ ] 테스트 커버리지 보강
