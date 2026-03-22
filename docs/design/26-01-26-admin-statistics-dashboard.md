# Admin 통계 대시보드 설계

## 1. 개요

### 1.1 목적
- 어드민 사용자 전용 통계 대시보드 구현
- Thymeleaf + Chart.js 기반 SSR 방식
- 유저, 이벤트, 고정 스케줄, 리텐션 관련 통계 시각화

### 1.2 요구사항
- Admin 권한(ROLE_ADMIN) 사용자만 접근 가능
- 기존 JWT 토큰 인증 체계 활용 (쿠키 저장 방식)
- Chart.js를 활용한 그래프/차트 시각화
- 월별/주별/요일별 시계열 데이터 지원
- 반응형 UI (Bootstrap 5)

### 1.3 기술 스택
| 구분 | 기술 | 비고 |
|------|------|------|
| 템플릿 엔진 | Thymeleaf 3.1 | Spring Boot 기본 |
| UI 템플릿 | **SB Admin 2** | Bootstrap 4 기반, MIT 라이선스 |
| CSS | Bootstrap 4.6 | SB Admin 2 내장 |
| 차트 | Chart.js 4 | SB Admin 2 내장 |
| 아이콘 | Font Awesome 5 | SB Admin 2 내장 |

### 1.4 UI 템플릿 선정 이유

**SB Admin 2 선택**
- 가볍고 심플 (AdminLTE 대비)
- Chart.js 기본 포함
- MIT 라이선스 (상업용 무료)
- Bootstrap 4 기반 (안정적)
- 커스터마이징 용이

**향후 AdminLTE 이관 가능**
- Thymeleaf Fragment 패턴으로 구현
- Fragment만 교체하면 3-4시간 내 이관 가능

**참고 링크**
- SB Admin 2: https://startbootstrap.com/theme/sb-admin-2
- GitHub: https://github.com/StartBootstrap/startbootstrap-sb-admin-2
- Demo: https://startbootstrap.github.io/startbootstrap-sb-admin-2/

---

## 2. 아키텍처 설계

### 2.1 레이어 구조

```
┌─────────────────────────────────────────────────────────┐
│                    AdminStatisticsController             │
│                    /api/v1/admin/statistics              │
└─────────────────────────┬───────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────┐
│                    StatisticsService                     │
│              (비즈니스 로직 + 캐싱 처리)                  │
└─────────────────────────┬───────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────┐
│              StatisticsRepository (QueryDSL)             │
│                    + Native Query                        │
└─────────────────────────────────────────────────────────┘
```

### 2.2 패키지 구조

```
src/main/java/side/onetime/
├── controller/
│   └── AdminStatisticsController.java
├── service/
│   └── StatisticsService.java
├── repository/
│   └── StatisticsRepository.java
├── dto/
│   └── admin/
│       └── statistics/
│           ├── request/
│           │   └── StatisticsRequest.java
│           └── response/
│               ├── UserStatisticsResponse.java
│               ├── EventStatisticsResponse.java
│               ├── ScheduleStatisticsResponse.java
│               ├── RetentionStatisticsResponse.java
│               └── ChartDataResponse.java
└── exception/
    └── status/
        └── StatisticsErrorStatus.java
```

---

## 3. API 엔드포인트 설계

### 3.1 기본 URL 구조
```
/api/v1/admin/statistics
```

### 3.2 공통 요청 파라미터
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|-------|------|
| startDate | String | N | 1년 전 | 조회 시작일 (yyyy-MM-dd) |
| endDate | String | N | 오늘 | 조회 종료일 (yyyy-MM-dd) |
| period | String | N | MONTHLY | 집계 기간 (DAILY, WEEKLY, MONTHLY) |

### 3.3 API 목록

#### 3.3.1 유저 통계 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/users/summary` | 유저 통계 요약 (전체 개요) |
| GET | `/users/signups` | 월별 신규 가입 유저 수 |
| GET | `/users/providers` | OAuth 제공자별 가입 유저 수 |
| GET | `/users/languages` | 언어 설정별 가입 유저 수 |
| GET | `/users/marketing-agreement` | 마케팅 동의율 |

#### 3.3.2 이벤트 통계 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/events/summary` | 이벤트 통계 요약 |
| GET | `/events/created` | 월별/주별 이벤트 생성 수 |
| GET | `/events/categories` | 이벤트 유형별 생성 비율 |
| GET | `/events/participants` | 월별 이벤트 참여자 수 (회원/비회원) |
| GET | `/events/avg-participants` | 이벤트당 평균 참여자 수 |
| GET | `/events/weekday-distribution` | 요일별 이벤트 생성 분포 |
| GET | `/events/start-times` | 이벤트 시작시간 TOP 10 |
| GET | `/events/keywords` | 이벤트 제목 키워드 분석 TOP 10 |
| GET | `/events/per-user` | 1인당 월평균 이벤트 생성 수 |

#### 3.3.3 스케줄 통계 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/schedules/fixed-users` | 월별 고정 스케줄 등록 유저 수 |
| GET | `/schedules/fixed-top-times` | 가장 많이 선택된 고정 시간대 TOP 10 |
| GET | `/schedules/fixed-heatmap` | 고정 스케줄 히트맵 (요일×시간) |
| GET | `/schedules/event-heatmap` | 이벤트 스케줄 히트맵 TOP 30 |

#### 3.3.4 리텐션/활동성 통계 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/retention/mau` | 월별 활성 유저 수 (MAU) - 이벤트 참여 기준 |
| GET | `/retention/mau-login` | 월별 활성 유저 수 - 로그인(refresh_token) 기준 |
| GET | `/retention/dau` | 일별 활성 유저 (회원 로그인 기준) |
| GET | `/retention/returning-users` | 재방문 유저 분석 |
| GET | `/retention/signup-to-event` | 가입 후 이벤트 생성 주기 분석 |
| GET | `/retention/dormant-users` | 휴면 유저 분석 (30일+ 미접속) |
| GET | `/retention/session-stats` | 유저별 세션/기기 분석 |

#### 3.3.5 마케팅 타겟 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/marketing/dormant-users` | 휴면 유저 리스트 (?days=30,60,90 필터) |
| GET | `/marketing/no-event-users` | 가입 후 이벤트 미생성 유저 (?daysAfterSignup=7) |
| GET | `/marketing/one-time-users` | 1회성 유저 리스트 (이벤트 1개만 생성) |
| GET | `/marketing/vip-users` | VIP 유저 리스트 (이벤트 5개+ 생성) |
| GET | `/marketing/zero-participant-events` | 참여자 0명 이벤트 + 주인 리스트 |
| GET | `/marketing/summary` | 마케팅 발송 가능 유저 수 요약 |

#### 3.3.6 배너 통계 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/banners/clicks` | 배너별 클릭 수 |
| GET | `/banners/ctr` | 배너별 클릭률 (CTR) |

#### 3.3.7 대시보드 통합 API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/dashboard` | 대시보드 전체 데이터 (요약 + 주요 차트) |
| GET | `/dashboard/season-comparison` | 시즌 비교 (개강 3,9월 vs 비시즌) |
| GET | `/dashboard/member-ratio` | 회원/비회원 참여 비율 |

---

## 4. 응답 포맷 설계

### 4.1 공통 응답 래퍼 (기존 ApiResponse 활용)

```java
{
  "is_success": true,
  "code": "COMMON-200",
  "message": "성공입니다.",
  "payload": { ... }
}
```

### 4.2 차트 데이터 응답 포맷 (Chart.js/Recharts 호환)

```java
public record ChartDataResponse(
    List<String> labels,           // X축 라벨 ["2025-01", "2025-02", ...]
    List<DatasetResponse> datasets // 데이터셋 목록
) {
    public record DatasetResponse(
        String label,              // 시리즈 이름
        List<Number> data,         // Y축 데이터 [100, 150, 200, ...]
        String borderColor,        // 선 색상 (optional)
        String backgroundColor     // 배경 색상 (optional)
    ) {}
}
```

### 4.3 시계열 데이터 응답 예시

```json
{
  "is_success": true,
  "code": "COMMON-200",
  "message": "성공입니다.",
  "payload": {
    "labels": ["2025-01", "2025-02", "2025-03", "2025-04"],
    "datasets": [
      {
        "label": "신규 가입 유저",
        "data": [120, 145, 180, 210]
      }
    ],
    "summary": {
      "total": 655,
      "average": 163.75,
      "max": 210,
      "min": 120
    }
  }
}
```

### 4.4 비율/분포 데이터 응답 예시 (파이/도넛 차트용)

```json
{
  "is_success": true,
  "code": "COMMON-200",
  "message": "성공입니다.",
  "payload": {
    "data": [
      { "label": "GOOGLE", "value": 450, "percentage": 45.0 },
      { "label": "KAKAO", "value": 350, "percentage": 35.0 },
      { "label": "NAVER", "value": 200, "percentage": 20.0 }
    ],
    "total": 1000
  }
}
```

### 4.5 히트맵 데이터 응답 예시

```json
{
  "is_success": true,
  "code": "COMMON-200",
  "message": "성공입니다.",
  "payload": {
    "xLabels": ["00:00", "01:00", "02:00", ...], // 시간
    "yLabels": ["MON", "TUE", "WED", ...],       // 요일
    "data": [
      { "x": 0, "y": 0, "value": 10 },
      { "x": 1, "y": 0, "value": 25 },
      ...
    ],
    "maxValue": 150
  }
}
```

### 4.6 대시보드 통합 응답 예시

```json
{
  "is_success": true,
  "code": "COMMON-200",
  "message": "성공입니다.",
  "payload": {
    "summary": {
      "totalUsers": 2115,
      "totalEvents": 2735,
      "mau": 492,
      "avgParticipantsPerEvent": 3.31,
      "memberToNonMemberRatio": "2:1",
      "marketingTargetUsers": 657,
      "dormantUsers": 1475,
      "dormantRate": 76.46
    },
    "charts": {
      "monthlySignups": { "labels": [...], "datasets": [...] },
      "eventCategories": { "data": [...] },
      "weekdayDistribution": { "data": [...] },
      "memberVsNonMember": { "data": [...] }
    },
    "seasonComparison": {
      "peakSeason": { "months": ["3월", "9월"], "avgSignups": 275 },
      "offSeason": { "months": ["8월", "12월"], "avgSignups": 117 },
      "ratio": 2.35
    },
    "topLists": {
      "keywords": [
        { "keyword": "회의", "count": 825 },
        { "keyword": "술", "count": 51 },
        { "keyword": "스터디", "count": 50 }
      ],
      "startTimes": [
        { "time": "09:00", "count": 1972, "percentage": 72.08 },
        { "time": "00:00", "count": 316, "percentage": 11.55 }
      ]
    },
    "generatedAt": "2025-01-26T10:30:00"
  }
}
```

---

## 5. 보안 설계

### 5.1 접근 제어

#### SecurityConfig 설정
```java
private static final String[] ADMIN_STATISTICS_URLS = {
    "/api/v1/admin/statistics/**"
};

@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    http.authorizeHttpRequests(authorize -> authorize
        .requestMatchers(ADMIN_STATISTICS_URLS).hasRole("ADMIN")
        // ...
    );
}
```

### 5.2 메서드 레벨 권한 검증

```java
@RestController
@RequestMapping("/api/v1/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    @GetMapping("/users/summary")
    public ResponseEntity<ApiResponse<UserStatisticsSummaryResponse>> getUserStatisticsSummary(
            @AuthenticationPrincipal CustomAdminDetails adminDetails,
            @Valid StatisticsRequest request) {

        // adminDetails로 로그인한 어드민 정보 확인 가능
        // AdminStatus 확인은 JwtFilter에서 이미 처리됨

        return ResponseEntity.ok(
            ApiResponse.onSuccess(statisticsService.getUserStatisticsSummary(request))
        );
    }
}
```

### 5.3 Admin 전용 인증 유틸

```java
// 기존 AdminAuthorizationUtil 활용
Long adminId = AdminAuthorizationUtil.getLoginAdminId();
```

---

## 6. 성능 최적화 전략

### 6.1 캐싱 전략

#### Spring Cache 적용
```java
@Service
@RequiredArgsConstructor
public class StatisticsService {

    @Cacheable(
        value = "statistics:users:summary",
        key = "#request.startDate + '-' + #request.endDate",
        unless = "#result == null"
    )
    public UserStatisticsSummaryResponse getUserStatisticsSummary(StatisticsRequest request) {
        // ...
    }
}
```

#### 캐시 설정 (application.yaml)
```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=10m
```

### 6.2 쿼리 최적화

#### QueryDSL 활용
```java
@Repository
@RequiredArgsConstructor
public class StatisticsRepositoryImpl implements StatisticsRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MonthlySignupDto> getMonthlySignups(LocalDate startDate, LocalDate endDate) {
        return queryFactory
            .select(Projections.constructor(MonthlySignupDto.class,
                Expressions.stringTemplate(
                    "DATE_FORMAT({0}, '%Y-%m')", user.createdDate
                ).as("month"),
                user.count().as("count"),
                new CaseBuilder()
                    .when(user.status.eq(Status.DELETED))
                    .then(1L)
                    .otherwise(0L)
                    .sum()
                    .as("deletedCount")
            ))
            .from(user)
            .where(
                user.createdDate.goe(startDate.atStartOfDay()),
                user.createdDate.lt(endDate.plusDays(1).atStartOfDay())
            )
            .groupBy(Expressions.stringTemplate("DATE_FORMAT({0}, '%Y-%m')", user.createdDate))
            .orderBy(Expressions.stringTemplate("DATE_FORMAT({0}, '%Y-%m')", user.createdDate).asc())
            .fetch();
    }
}
```

### 6.3 Native Query 활용 (복잡한 집계)

```java
@Repository
public interface StatisticsRepository extends JpaRepository<User, Long> {

    @Query(value = """
        SELECT
            DATE_FORMAT(created_date, '%Y-%m') AS month,
            COUNT(*) AS new_users,
            SUM(CASE WHEN status = 'DELETED' THEN 1 ELSE 0 END) AS deleted_count
        FROM users
        WHERE created_date >= :startDate
          AND created_date < :endDate
        GROUP BY DATE_FORMAT(created_date, '%Y-%m')
        ORDER BY month
        """, nativeQuery = true)
    List<Object[]> findMonthlySignups(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
```

---

## 7. DTO 설계

### 7.1 요청 DTO

```java
public record StatisticsRequest(
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    LocalDate startDate,

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    LocalDate endDate,

    Period period  // DAILY, WEEKLY, MONTHLY
) {
    public StatisticsRequest {
        if (startDate == null) {
            startDate = LocalDate.now().minusYears(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (period == null) {
            period = Period.MONTHLY;
        }
    }

    public enum Period {
        DAILY, WEEKLY, MONTHLY
    }
}
```

### 7.2 응답 DTO 예시

```java
// 유저 통계 요약
public record UserStatisticsSummaryResponse(
    long totalUsers,
    long activeUsers,
    long deletedUsers,
    double marketingAgreementRate,
    Map<String, Long> providerDistribution,
    Map<String, Long> languageDistribution
) {}

// 시계열 차트 데이터
public record TimeSeriesChartResponse(
    List<String> labels,
    List<DatasetDto> datasets,
    SummaryDto summary
) {
    public record DatasetDto(
        String label,
        List<Long> data
    ) {}

    public record SummaryDto(
        long total,
        double average,
        long max,
        long min
    ) {}
}

// 분포 차트 데이터
public record DistributionChartResponse(
    List<DistributionItemDto> data,
    long total
) {
    public record DistributionItemDto(
        String label,
        long value,
        double percentage
    ) {}
}

// 히트맵 데이터
public record HeatmapResponse(
    List<String> xLabels,
    List<String> yLabels,
    List<HeatmapPointDto> data,
    long maxValue
) {
    public record HeatmapPointDto(
        int x,
        int y,
        long value
    ) {}
}
```

---

## 8. 에러 처리

### 8.1 통계 전용 에러 상태

```java
@Getter
@RequiredArgsConstructor
public enum StatisticsErrorStatus implements BaseErrorCode {

    _INVALID_DATE_RANGE(BAD_REQUEST, "STAT-001", "유효하지 않은 날짜 범위입니다."),
    _DATE_RANGE_TOO_LONG(BAD_REQUEST, "STAT-002", "조회 기간은 최대 2년까지 가능합니다."),
    _INVALID_PERIOD_TYPE(BAD_REQUEST, "STAT-003", "유효하지 않은 집계 기간입니다."),
    _STATISTICS_NOT_AVAILABLE(SERVICE_UNAVAILABLE, "STAT-004", "통계 데이터를 조회할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
```

---

## 9. 프론트엔드 통합 가이드

### 9.1 Chart.js 연동 예시

```javascript
// React에서 Chart.js 사용
const response = await fetch('/api/v1/admin/statistics/users/signups', {
  headers: { Authorization: `Bearer ${token}` }
});
const { payload } = await response.json();

new Chart(ctx, {
  type: 'line',
  data: {
    labels: payload.labels,
    datasets: payload.datasets.map(ds => ({
      label: ds.label,
      data: ds.data,
      borderColor: ds.borderColor || '#4F46E5',
      tension: 0.1
    }))
  }
});
```

### 9.2 Recharts 연동 예시

```jsx
// Recharts 사용
const { payload } = await response.json();

// 데이터 변환
const chartData = payload.labels.map((label, idx) => ({
  name: label,
  ...payload.datasets.reduce((acc, ds) => ({
    ...acc,
    [ds.label]: ds.data[idx]
  }), {})
}));

<LineChart data={chartData}>
  <XAxis dataKey="name" />
  <YAxis />
  {payload.datasets.map(ds => (
    <Line key={ds.label} type="monotone" dataKey={ds.label} />
  ))}
</LineChart>
```

---

## 10. 구현 우선순위

### Phase 1: 핵심 통계 + 대시보드 (MVP)
1. 대시보드 통합 API (`/dashboard`)
2. 월별 신규 가입 유저 수
3. 월별 이벤트 생성 수
4. 회원/비회원 참여 비율
5. MAU + 휴면율
6. 마케팅 발송 가능 유저 수
7. Thymeleaf 로그인 + 대시보드 페이지

### Phase 2: 상세 통계 + 마케팅 타겟
1. OAuth 제공자별 통계
2. 이벤트 참여자 통계 (회원/비회원 분리)
3. 요일별 분포 + 시간대 분석
4. 키워드 분석 TOP 10
5. 시즌 비교 (개강 vs 비시즌)
6. **마케팅 타겟 리스트 API**
   - 휴면 유저 (30/60/90일 필터)
   - 가입 후 이벤트 미생성 유저
   - 참여자 0명 이벤트 주인

### Phase 3: 고급 분석 + 리텐션
1. 스케줄 히트맵 (바쁜 시간 / 가능한 시간)
2. 리텐션 상세 분석
3. 가입→이벤트 생성 주기 (Time to Value)
4. VIP 유저 / 1회성 유저 분류
5. refresh_token 기반 DAU (회원 접속 추적)

---

## 11. 테스트 전략

### 11.1 단위 테스트
- Service 레이어 테스트 (Mockito)
- Repository 쿼리 테스트 (@DataJpaTest)

### 11.2 통합 테스트
- Controller 테스트 (MockMvc)
- 인증/인가 테스트

### 11.3 API 문서화
- Spring REST Docs로 문서 생성
- Swagger UI 연동

---

## 12. 참고 사항

### 12.1 기존 코드 참조
- 어드민 인증: `CustomAdminDetails`, `CustomAdminDetailsService`
- JWT 처리: `JwtUtil`, `JwtFilter`
- 권한 확인: `AdminAuthorizationUtil`
- 에러 처리: `GlobalExceptionHandler`

### 12.2 참고 프로젝트
- socc-assistant-api의 StatisticsMapper.xml 패턴
  - 날짜 조건 공통화 (`<sql id="dateTimeCondition">`)
  - 캐싱 설정 (`<cache eviction="LRU" flushInterval="60000">`)
  - 시간별/일별/월별 분리 쿼리

### 12.3 추가 통계 쿼리 (refresh_token, 마케팅 타겟)

#### 활성 유저 (회원 로그인 기준)
```sql
-- DAU (refresh_token 기준 - 회원만)
SELECT DATE(last_used_at) AS date, COUNT(DISTINCT users_id) AS dau
FROM refresh_token
WHERE last_used_at >= :startDate AND last_used_at < :endDate
  AND status = 'ACTIVE'
GROUP BY DATE(last_used_at)
ORDER BY date;

-- 시간대별 접속 분포
SELECT HOUR(last_used_at) AS hour, COUNT(*) AS count
FROM refresh_token
WHERE last_used_at >= :startDate AND last_used_at < :endDate
GROUP BY HOUR(last_used_at)
ORDER BY hour;
```

#### 마케팅 타겟: 휴면 유저 (기간별 필터)
```sql
-- 휴면 유저 (마케팅 동의자만, ?days 파라미터로 필터)
-- days=30: 30일 이상, days=60: 60일 이상, days=90: 90일 이상
SELECT u.users_id, u.email, u.nickname, u.language,
       MAX(rt.last_used_at) AS last_login,
       DATEDIFF(NOW(), MAX(rt.last_used_at)) AS days_inactive
FROM users u
LEFT JOIN refresh_token rt ON u.users_id = rt.users_id AND rt.status = 'ACTIVE'
WHERE u.status = 'ACTIVE'
  AND u.marketing_policy_agreement = 1
GROUP BY u.users_id
HAVING days_inactive > :days OR last_login IS NULL
ORDER BY days_inactive DESC;

-- 휴면 기간별 분포 (대시보드용)
SELECT
    CASE
        WHEN days_inactive BETWEEN 30 AND 59 THEN '30-59일'
        WHEN days_inactive BETWEEN 60 AND 89 THEN '60-89일'
        WHEN days_inactive >= 90 THEN '90일+'
        ELSE '활성'
    END AS dormant_group,
    COUNT(*) AS user_count
FROM (
    SELECT u.users_id, DATEDIFF(NOW(), MAX(rt.last_used_at)) AS days_inactive
    FROM users u
    LEFT JOIN refresh_token rt ON u.users_id = rt.users_id
    WHERE u.status = 'ACTIVE'
    GROUP BY u.users_id
) sub
GROUP BY dormant_group;
```

#### 마케팅 타겟: 가입 후 이벤트 미생성 유저
```sql
-- 가입 7일+ 경과, 이벤트 생성 0건 (마케팅 동의자만)
SELECT u.users_id, u.email, u.nickname, u.language, u.created_date,
       DATEDIFF(NOW(), u.created_date) AS days_since_signup
FROM users u
LEFT JOIN event_participations ep ON u.users_id = ep.users_id
    AND ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
WHERE u.status = 'ACTIVE'
  AND u.marketing_policy_agreement = 1
  AND ep.users_id IS NULL
  AND u.created_date < DATE_SUB(NOW(), INTERVAL 7 DAY)
ORDER BY u.created_date;
```

#### 마케팅 타겟: 1회성 유저
```sql
-- 이벤트 1개만 생성한 유저 (재사용 유도 대상)
SELECT u.users_id, u.email, u.nickname, u.language, COUNT(ep.events_id) AS event_count
FROM users u
JOIN event_participations ep ON u.users_id = ep.users_id
    AND ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
WHERE u.status = 'ACTIVE'
  AND u.marketing_policy_agreement = 1
GROUP BY u.users_id
HAVING event_count = 1;
```

#### 마케팅 타겟: 참여자 0명 이벤트
```sql
-- 이벤트 생성했지만 참여자(비회원)가 없는 경우
SELECT u.users_id, u.email, e.events_id, e.title, e.created_date,
       DATEDIFF(NOW(), e.created_date) AS days_since_created
FROM users u
JOIN event_participations ep ON u.users_id = ep.users_id
JOIN events e ON ep.events_id = e.events_id
LEFT JOIN members m ON e.events_id = m.events_id
WHERE ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
  AND e.status = 'ACTIVE'
  AND u.marketing_policy_agreement = 1
  AND e.created_date < DATE_SUB(NOW(), INTERVAL 3 DAY)  -- 3일 이상 경과
GROUP BY e.events_id
HAVING COUNT(m.members_id) = 0
ORDER BY e.created_date;
```

#### VIP 유저 (이벤트 5개+ 생성)
```sql
SELECT u.users_id, u.email, u.nickname, COUNT(ep.events_id) AS event_count
FROM users u
JOIN event_participations ep ON u.users_id = ep.users_id
    AND ep.event_status IN ('CREATOR', 'CREATOR_AND_PARTICIPANT')
WHERE u.status = 'ACTIVE'
GROUP BY u.users_id
HAVING event_count >= 5
ORDER BY event_count DESC;
```

### 12.4 2025년 데이터 분석 인사이트

#### 핵심 지표 (2025년 기준)
| 지표 | 수치 | 의미 |
|------|------|------|
| 연간 신규 가입 | 2,115명 | 전년 대비 대폭 증가 |
| 연간 이벤트 생성 | 2,735개 | - |
| 평균 참여자/이벤트 | 3.31명 | - |
| 회원:비회원 참여 | 2:1 | 6,007 vs 2,967 |
| 재방문율 | 41.78% | 업계 평균 수준 |
| 휴면율 (60일+) | 76.46% | 일회성 사용 후 이탈 많음 |
| 가입→첫 이벤트 | 37일 | Time to Value 느림 |
| 마케팅 동의율 | 31.28% | 개선 필요 |

#### 시즌별 패턴
| 시즌 | 특징 |
|------|------|
| **개강 (3월, 9월)** | 가입/이벤트 급증, 고정스케줄 등록 집중 |
| **방학 (8월, 12월)** | 가입 감소, but 이벤트당 참여자↑ (MT, 여행) |
| **중간 (4-6월, 10-11월)** | 안정적 사용 |

#### 사용 패턴
- **목요일** 이벤트 생성 최다 (주말 약속 미리 잡기)
- **저녁 19:30~22:00** 가장 인기 있는 가능 시간대
- **09:00 시작** 72% (디폴트 값 거의 안 바꿈)
- **"회의"** 키워드 압도적 1위 (30%)

#### 마케팅 포인트
1. **개강 시즌 집중 마케팅** - 3월/9월 전에 준비
2. **취준생 타겟팅** - 면접 스터디 일정 조율 니즈
3. **휴면 유저 리타겟팅** - 개강 시즌에 복귀 유도 이메일
4. **가입 후 빠른 온보딩** - 웰컴 메일 + 7일 후 리마인드

### 12.5 비회원 포함 활성 지표 참고

> **참고**: 비회원(member) 사용 비율이 높으므로 refresh_token 기반 MAU는 회원만 반영.
> 전체 서비스 활성도는 `이벤트 생성 수` + `참여자(members) 수` 기준으로 별도 집계 필요.

```sql
-- 전체 서비스 활성 지표 (회원+비회원)
SELECT
    DATE_FORMAT(created_date, '%Y-%m') AS month,
    (SELECT COUNT(*) FROM events WHERE ...) AS events_created,
    (SELECT COUNT(*) FROM members WHERE ...) AS members_participated,
    (SELECT COUNT(*) FROM event_participations WHERE ...) AS users_participated
FROM ...
```

---

## 13. UI 설계 (SB Admin 2 + Thymeleaf + Chart.js)

### 13.1 의존성 추가

```groovy
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
implementation 'nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.3.0'  // 레이아웃 지원
```

### 13.2 SB Admin 2 설치

```bash
# 1. SB Admin 2 다운로드
# https://startbootstrap.com/theme/sb-admin-2 에서 다운로드

# 2. 압축 해제 후 static/admin/ 폴더로 복사
# - css/sb-admin-2.min.css
# - js/sb-admin-2.min.js
# - vendor/ (Bootstrap, jQuery, Chart.js 포함)
```

### 13.3 디렉토리 구조

```
src/main/resources/
├── templates/
│   └── admin/
│       ├── fragments/                    # ★ Fragment 패턴 (이관 용이)
│       │   ├── header.html              # <head> 영역 (CSS, meta)
│       │   ├── sidebar.html             # 좌측 사이드바
│       │   ├── topbar.html              # 상단바 (로그아웃, 알림)
│       │   └── footer.html              # 푸터 + JS 로드
│       ├── layout/
│       │   └── default.html             # 공통 레이아웃 (fragments 조합)
│       ├── login.html                   # 어드민 로그인
│       ├── dashboard.html               # 메인 대시보드
│       ├── users.html                   # 유저 통계 상세
│       ├── events.html                  # 이벤트 통계 상세
│       ├── retention.html               # 리텐션 분석
│       └── marketing.html               # 마케팅 타겟 리스트
└── static/
    └── admin/
        ├── css/
        │   ├── sb-admin-2.min.css       # SB Admin 2 스타일
        │   └── custom.css               # 커스텀 스타일
        ├── js/
        │   ├── sb-admin-2.min.js        # SB Admin 2 JS
        │   └── charts-config.js         # Chart.js 공통 설정
        └── vendor/                       # SB Admin 2 벤더 라이브러리
            ├── bootstrap/
            ├── jquery/
            ├── chart.js/
            └── fontawesome-free/
```

### 13.4 Fragment 패턴 (AdminLTE 이관 대비)

```
┌─────────────────────────────────────────────────────────────┐
│ layout/default.html                                          │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ th:replace="~{fragments/header :: head}"                │ │  ← CSS, meta
│ └─────────────────────────────────────────────────────────┘ │
│ ┌───────────┐ ┌─────────────────────────────────────────┐   │
│ │ sidebar   │ │ topbar                                  │   │
│ │ fragment  │ │ fragment                                │   │
│ │           │ ├─────────────────────────────────────────┤   │
│ │           │ │                                         │   │
│ │           │ │  layout:fragment="content"              │   │  ← 페이지별 콘텐츠
│ │           │ │                                         │   │
│ │           │ └─────────────────────────────────────────┤   │
│ └───────────┘ │ footer fragment                         │   │
│               └─────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘

★ AdminLTE 이관 시: fragments/ 폴더 내 4개 파일만 교체
```

### 13.3 페이지 구성

| 페이지 | URL | 설명 |
|--------|-----|------|
| 로그인 | `/admin/login` | 어드민 로그인 폼 |
| 대시보드 | `/admin/dashboard` | 핵심 지표 요약 + 주요 차트 |
| 유저 통계 | `/admin/statistics/users` | 유저 관련 상세 차트 |
| 이벤트 통계 | `/admin/statistics/events` | 이벤트 관련 상세 차트 |
| 리텐션 | `/admin/statistics/retention` | 활동성/리텐션 분석 |

### 13.4 인증 방식

**JWT를 HttpOnly 쿠키로 저장하는 방식 사용**

```
┌──────────────┐     POST /admin/login      ┌──────────────┐
│  로그인 폼   │ ─────────────────────────► │    서버      │
│              │                            │              │
│              │ ◄───────────────────────── │ Set-Cookie:  │
│              │   HttpOnly Cookie (JWT)    │ admin_token  │
└──────────────┘                            └──────────────┘
        │
        │ 이후 모든 요청
        ▼
┌──────────────┐     Cookie: admin_token    ┌──────────────┐
│  대시보드    │ ─────────────────────────► │    서버      │
│              │                            │ JWT 검증     │
└──────────────┘                            └──────────────┘
```

### 13.5 컨트롤러 구조

```java
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPageController {

    private final AdminService adminService;
    private final StatisticsService statisticsService;

    // ==================== 인증 ====================

    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String name,
                        @RequestParam String password,
                        HttpServletResponse response,
                        RedirectAttributes redirectAttributes) {
        try {
            LoginAdminUserResponse result = adminService.loginAdminUser(
                new LoginAdminUserRequest(name, password)
            );

            // JWT를 HttpOnly 쿠키로 저장
            Cookie cookie = new Cookie("admin_token", result.accessToken());
            cookie.setHttpOnly(true);
            cookie.setPath("/admin");
            cookie.setMaxAge(60 * 60 * 24); // 1일
            response.addCookie(cookie);

            return "redirect:/admin/dashboard";
        } catch (CustomException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        // 쿠키 삭제
        Cookie cookie = new Cookie("admin_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/admin");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return "redirect:/admin/login";
    }

    // ==================== 대시보드 ====================

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("summary", statisticsService.getDashboardSummary());
        model.addAttribute("charts", statisticsService.getDashboardCharts());
        return "admin/dashboard";
    }

    @GetMapping("/statistics/users")
    public String usersStatistics(Model model, StatisticsRequest request) {
        model.addAttribute("data", statisticsService.getUserStatistics(request));
        return "admin/users";
    }

    @GetMapping("/statistics/events")
    public String eventsStatistics(Model model, StatisticsRequest request) {
        model.addAttribute("data", statisticsService.getEventStatistics(request));
        return "admin/events";
    }

    @GetMapping("/statistics/retention")
    public String retentionStatistics(Model model, StatisticsRequest request) {
        model.addAttribute("data", statisticsService.getRetentionStatistics(request));
        return "admin/retention";
    }
}
```

### 13.6 Fragment 파일들

#### fragments/header.html
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:fragment="head">
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>OneTime Admin</title>

    <!-- SB Admin 2 CSS -->
    <link th:href="@{/admin/vendor/fontawesome-free/css/all.min.css}" rel="stylesheet">
    <link th:href="@{/admin/css/sb-admin-2.min.css}" rel="stylesheet">
    <link th:href="@{/admin/css/custom.css}" rel="stylesheet">
</head>
</html>
```

#### fragments/sidebar.html
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<ul th:fragment="sidebar" class="navbar-nav bg-gradient-primary sidebar sidebar-dark accordion" id="accordionSidebar">
    <!-- Brand -->
    <a class="sidebar-brand d-flex align-items-center justify-content-center" th:href="@{/admin/dashboard}">
        <div class="sidebar-brand-text mx-3">OneTime Admin</div>
    </a>
    <hr class="sidebar-divider my-0">

    <!-- Dashboard -->
    <li class="nav-item" th:classappend="${currentPage == 'dashboard'} ? 'active'">
        <a class="nav-link" th:href="@{/admin/dashboard}">
            <i class="fas fa-fw fa-tachometer-alt"></i>
            <span>대시보드</span>
        </a>
    </li>

    <hr class="sidebar-divider">
    <div class="sidebar-heading">통계</div>

    <!-- 유저 통계 -->
    <li class="nav-item" th:classappend="${currentPage == 'users'} ? 'active'">
        <a class="nav-link" th:href="@{/admin/statistics/users}">
            <i class="fas fa-fw fa-users"></i>
            <span>유저 통계</span>
        </a>
    </li>

    <!-- 이벤트 통계 -->
    <li class="nav-item" th:classappend="${currentPage == 'events'} ? 'active'">
        <a class="nav-link" th:href="@{/admin/statistics/events}">
            <i class="fas fa-fw fa-calendar-alt"></i>
            <span>이벤트 통계</span>
        </a>
    </li>

    <!-- 리텐션 -->
    <li class="nav-item" th:classappend="${currentPage == 'retention'} ? 'active'">
        <a class="nav-link" th:href="@{/admin/statistics/retention}">
            <i class="fas fa-fw fa-chart-line"></i>
            <span>리텐션</span>
        </a>
    </li>

    <hr class="sidebar-divider">
    <div class="sidebar-heading">마케팅</div>

    <!-- 마케팅 타겟 -->
    <li class="nav-item" th:classappend="${currentPage == 'marketing'} ? 'active'">
        <a class="nav-link" th:href="@{/admin/statistics/marketing}">
            <i class="fas fa-fw fa-bullhorn"></i>
            <span>마케팅 타겟</span>
        </a>
    </li>

    <hr class="sidebar-divider d-none d-md-block">
    <!-- Sidebar Toggler -->
    <div class="text-center d-none d-md-inline">
        <button class="rounded-circle border-0" id="sidebarToggle"></button>
    </div>
</ul>
</html>
```

#### fragments/topbar.html
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<nav th:fragment="topbar" class="navbar navbar-expand navbar-light bg-white topbar mb-4 static-top shadow">
    <!-- Sidebar Toggle (Topbar) -->
    <button id="sidebarToggleTop" class="btn btn-link d-md-none rounded-circle mr-3">
        <i class="fa fa-bars"></i>
    </button>

    <!-- Page Title -->
    <h1 class="h5 mb-0 text-gray-800" th:text="${pageTitle ?: '대시보드'}">대시보드</h1>

    <!-- Topbar Navbar -->
    <ul class="navbar-nav ml-auto">
        <!-- 날짜 표시 -->
        <li class="nav-item dropdown no-arrow mx-1">
            <span class="nav-link text-gray-600" th:text="${#temporals.format(#temporals.createNow(), 'yyyy-MM-dd')}">
                2025-01-26
            </span>
        </li>

        <div class="topbar-divider d-none d-sm-block"></div>

        <!-- User Dropdown -->
        <li class="nav-item dropdown no-arrow">
            <a class="nav-link dropdown-toggle" href="#" id="userDropdown" role="button"
               data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                <span class="mr-2 d-none d-lg-inline text-gray-600 small" th:text="${adminName ?: 'Admin'}">Admin</span>
                <i class="fas fa-user-circle fa-fw"></i>
            </a>
            <div class="dropdown-menu dropdown-menu-right shadow animated--grow-in">
                <a class="dropdown-item" th:href="@{/admin/logout}">
                    <i class="fas fa-sign-out-alt fa-sm fa-fw mr-2 text-gray-400"></i>
                    로그아웃
                </a>
            </div>
        </li>
    </ul>
</nav>
</html>
```

#### fragments/footer.html
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<div th:fragment="footer">
    <!-- Footer -->
    <footer class="sticky-footer bg-white">
        <div class="container my-auto">
            <div class="copyright text-center my-auto">
                <span>Copyright &copy; OneTime Admin 2025</span>
            </div>
        </div>
    </footer>
</div>

<th:block th:fragment="scripts">
    <!-- Bootstrap core JavaScript -->
    <script th:src="@{/admin/vendor/jquery/jquery.min.js}"></script>
    <script th:src="@{/admin/vendor/bootstrap/js/bootstrap.bundle.min.js}"></script>
    <!-- Core plugin JavaScript -->
    <script th:src="@{/admin/vendor/jquery-easing/jquery.easing.min.js}"></script>
    <!-- Custom scripts for SB Admin 2 -->
    <script th:src="@{/admin/js/sb-admin-2.min.js}"></script>
    <!-- Chart.js -->
    <script th:src="@{/admin/vendor/chart.js/Chart.min.js}"></script>
</th:block>
</html>
```

### 13.7 레이아웃 템플릿 (SB Admin 2 구조)

```html
<!-- templates/admin/layout/default.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout">
<head th:replace="~{admin/fragments/header :: head}"></head>
<body id="page-top">
    <!-- Page Wrapper -->
    <div id="wrapper">
        <!-- Sidebar -->
        <div th:replace="~{admin/fragments/sidebar :: sidebar}"></div>

        <!-- Content Wrapper -->
        <div id="content-wrapper" class="d-flex flex-column">
            <!-- Main Content -->
            <div id="content">
                <!-- Topbar -->
                <div th:replace="~{admin/fragments/topbar :: topbar}"></div>

                <!-- Begin Page Content -->
                <div class="container-fluid">
                    <!-- ★ 페이지별 콘텐츠가 여기에 삽입됨 -->
                    <div layout:fragment="content"></div>
                </div>
            </div>

            <!-- Footer -->
            <div th:replace="~{admin/fragments/footer :: footer}"></div>
        </div>
    </div>

    <!-- Scripts -->
    <th:block th:replace="~{admin/fragments/footer :: scripts}"></th:block>
    <!-- 페이지별 추가 스크립트 -->
    <th:block layout:fragment="scripts"></th:block>
</body>
</html>
```

### 13.8 로그인 페이지 (SB Admin 2 스타일)

```html
<!-- templates/admin/login.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>OneTime Admin - 로그인</title>
    <link th:href="@{/admin/vendor/fontawesome-free/css/all.min.css}" rel="stylesheet">
    <link th:href="@{/admin/css/sb-admin-2.min.css}" rel="stylesheet">
</head>
<body class="bg-gradient-primary">
    <div class="container">
        <div class="row justify-content-center">
            <div class="col-xl-6 col-lg-7 col-md-9">
                <div class="card o-hidden border-0 shadow-lg my-5">
                    <div class="card-body p-0">
                        <div class="p-5">
                            <div class="text-center">
                                <h1 class="h4 text-gray-900 mb-4">OneTime Admin</h1>
                            </div>

                            <!-- 에러 메시지 -->
                            <div th:if="${error}" class="alert alert-danger" th:text="${error}">
                                로그인 실패
                            </div>

                            <form class="user" method="post" th:action="@{/admin/login}">
                                <div class="form-group">
                                    <input type="text" name="name" class="form-control form-control-user"
                                           placeholder="아이디" required>
                                </div>
                                <div class="form-group">
                                    <input type="password" name="password" class="form-control form-control-user"
                                           placeholder="비밀번호" required>
                                </div>
                                <button type="submit" class="btn btn-primary btn-user btn-block">
                                    로그인
                                </button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script th:src="@{/admin/vendor/jquery/jquery.min.js}"></script>
    <script th:src="@{/admin/vendor/bootstrap/js/bootstrap.bundle.min.js}"></script>
    <script th:src="@{/admin/js/sb-admin-2.min.js}"></script>
</body>
</html>
```

### 13.9 대시보드 화면 (SB Admin 2 스타일)

```html
<!-- templates/admin/dashboard.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{admin/layout/default}">
<head>
    <title>대시보드 - OneTime Admin</title>
</head>
<body>
<div layout:fragment="content">
    <!-- Page Heading -->
    <div class="d-sm-flex align-items-center justify-content-between mb-4">
        <h1 class="h3 mb-0 text-gray-800">대시보드</h1>
        <span class="text-muted" th:text="'데이터 기준: ' + ${summary.generatedAt}"></span>
    </div>

    <!-- 요약 카드 Row -->
    <div class="row">
        <!-- 총 유저 카드 -->
        <div class="col-xl-3 col-md-6 mb-4">
            <div class="card border-left-primary shadow h-100 py-2">
                <div class="card-body">
                    <div class="row no-gutters align-items-center">
                        <div class="col mr-2">
                            <div class="text-xs font-weight-bold text-primary text-uppercase mb-1">총 유저 수</div>
                            <div class="h5 mb-0 font-weight-bold text-gray-800"
                                 th:text="${#numbers.formatInteger(summary.totalUsers, 0, 'COMMA')}">0</div>
                        </div>
                        <div class="col-auto">
                            <i class="fas fa-users fa-2x text-gray-300"></i>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 총 이벤트 카드 -->
        <div class="col-xl-3 col-md-6 mb-4">
            <div class="card border-left-success shadow h-100 py-2">
                <div class="card-body">
                    <div class="row no-gutters align-items-center">
                        <div class="col mr-2">
                            <div class="text-xs font-weight-bold text-success text-uppercase mb-1">총 이벤트 수</div>
                            <div class="h5 mb-0 font-weight-bold text-gray-800"
                                 th:text="${#numbers.formatInteger(summary.totalEvents, 0, 'COMMA')}">0</div>
                        </div>
                        <div class="col-auto">
                            <i class="fas fa-calendar-alt fa-2x text-gray-300"></i>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- MAU 카드 -->
        <div class="col-xl-3 col-md-6 mb-4">
            <div class="card border-left-info shadow h-100 py-2">
                <div class="card-body">
                    <div class="row no-gutters align-items-center">
                        <div class="col mr-2">
                            <div class="text-xs font-weight-bold text-info text-uppercase mb-1">MAU (이번 달)</div>
                            <div class="h5 mb-0 font-weight-bold text-gray-800"
                                 th:text="${#numbers.formatInteger(summary.mau, 0, 'COMMA')}">0</div>
                        </div>
                        <div class="col-auto">
                            <i class="fas fa-chart-line fa-2x text-gray-300"></i>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 휴면율 카드 -->
        <div class="col-xl-3 col-md-6 mb-4">
            <div class="card border-left-warning shadow h-100 py-2">
                <div class="card-body">
                    <div class="row no-gutters align-items-center">
                        <div class="col mr-2">
                            <div class="text-xs font-weight-bold text-warning text-uppercase mb-1">휴면율 (60일+)</div>
                            <div class="h5 mb-0 font-weight-bold text-gray-800"
                                 th:text="${#numbers.formatDecimal(summary.dormantRate, 1, 1) + '%'}">0%</div>
                        </div>
                        <div class="col-auto">
                            <i class="fas fa-bed fa-2x text-gray-300"></i>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- 차트 Row -->
    <div class="row">
        <!-- 월별 가입 차트 (Area Chart) -->
        <div class="col-xl-8 col-lg-7">
            <div class="card shadow mb-4">
                <div class="card-header py-3 d-flex flex-row align-items-center justify-content-between">
                    <h6 class="m-0 font-weight-bold text-primary">월별 신규 가입 유저</h6>
                </div>
                <div class="card-body">
                    <div class="chart-area">
                        <canvas id="signupsChart"></canvas>
                    </div>
                </div>
            </div>
        </div>

        <!-- OAuth 제공자 비율 (Pie Chart) -->
        <div class="col-xl-4 col-lg-5">
            <div class="card shadow mb-4">
                <div class="card-header py-3 d-flex flex-row align-items-center justify-content-between">
                    <h6 class="m-0 font-weight-bold text-primary">OAuth 제공자 비율</h6>
                </div>
                <div class="card-body">
                    <div class="chart-pie pt-4 pb-2">
                        <canvas id="providersChart"></canvas>
                    </div>
                    <div class="mt-4 text-center small">
                        <span class="mr-2"><i class="fas fa-circle text-danger"></i> Google</span>
                        <span class="mr-2"><i class="fas fa-circle text-warning"></i> Kakao</span>
                        <span class="mr-2"><i class="fas fa-circle text-success"></i> Naver</span>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- 추가 통계 Row -->
    <div class="row">
        <!-- 요일별 이벤트 생성 (Bar Chart) -->
        <div class="col-xl-6 col-lg-6">
            <div class="card shadow mb-4">
                <div class="card-header py-3">
                    <h6 class="m-0 font-weight-bold text-primary">요일별 이벤트 생성</h6>
                </div>
                <div class="card-body">
                    <div class="chart-bar">
                        <canvas id="weekdayChart"></canvas>
                    </div>
                </div>
            </div>
        </div>

        <!-- TOP 키워드 (Table) -->
        <div class="col-xl-6 col-lg-6">
            <div class="card shadow mb-4">
                <div class="card-header py-3">
                    <h6 class="m-0 font-weight-bold text-primary">이벤트 제목 키워드 TOP 10</h6>
                </div>
                <div class="card-body">
                    <div class="table-responsive">
                        <table class="table table-bordered table-sm">
                            <thead>
                                <tr>
                                    <th>순위</th>
                                    <th>키워드</th>
                                    <th>개수</th>
                                    <th>비율</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr th:each="item, stat : ${charts.topKeywords}">
                                    <td th:text="${stat.index + 1}">1</td>
                                    <td th:text="${item.keyword}">회의</td>
                                    <td th:text="${item.count}">825</td>
                                    <td th:text="${#numbers.formatDecimal(item.percentage, 1, 1) + '%'}">30.2%</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- 페이지 전용 스크립트 -->
<th:block layout:fragment="scripts">
<script th:inline="javascript">
    // Chart.js 데이터 (Thymeleaf에서 전달)
    const signupsData = /*[[${charts.monthlySignups}]]*/ {labels: [], data: []};
    const providersData = /*[[${charts.providers}]]*/ {labels: [], data: []};
    const weekdayData = /*[[${charts.weekdayDistribution}]]*/ {labels: [], data: []};

    // 컬러 팔레트 (SB Admin 2 스타일)
    const colors = {
        primary: '#4e73df',
        success: '#1cc88a',
        info: '#36b9cc',
        warning: '#f6c23e',
        danger: '#e74a3b'
    };

    // 월별 가입 Area Chart
    new Chart(document.getElementById('signupsChart'), {
        type: 'line',
        data: {
            labels: signupsData.labels,
            datasets: [{
                label: '신규 가입',
                data: signupsData.data,
                backgroundColor: 'rgba(78, 115, 223, 0.05)',
                borderColor: colors.primary,
                pointBackgroundColor: colors.primary,
                pointBorderColor: colors.primary,
                pointHoverBackgroundColor: colors.primary,
                pointHoverBorderColor: colors.primary,
                fill: true,
                tension: 0.3
            }]
        },
        options: {
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            scales: {
                y: { beginAtZero: true }
            }
        }
    });

    // OAuth 제공자 Doughnut Chart
    new Chart(document.getElementById('providersChart'), {
        type: 'doughnut',
        data: {
            labels: providersData.labels,
            datasets: [{
                data: providersData.data,
                backgroundColor: [colors.danger, colors.warning, colors.success],
                hoverBorderColor: 'rgba(234, 236, 244, 1)'
            }]
        },
        options: {
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            cutout: '70%'
        }
    });

    // 요일별 Bar Chart
    new Chart(document.getElementById('weekdayChart'), {
        type: 'bar',
        data: {
            labels: weekdayData.labels,
            datasets: [{
                label: '이벤트 수',
                data: weekdayData.data,
                backgroundColor: colors.primary,
                borderColor: colors.primary,
                borderWidth: 1
            }]
        },
        options: {
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            scales: {
                y: { beginAtZero: true }
            }
        }
    });
</script>
</th:block>
</body>
</html>
```

### 13.8 Security 설정 추가

```java
// SecurityConfig에 추가
private static final String[] ADMIN_PAGE_URLS = {
    "/admin/**"
};

private static final String[] ADMIN_PUBLIC_URLS = {
    "/admin/login",
    "/admin/css/**",
    "/admin/js/**"
};

@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    http.authorizeHttpRequests(authorize -> authorize
        .requestMatchers(ADMIN_PUBLIC_URLS).permitAll()
        .requestMatchers(ADMIN_PAGE_URLS).hasRole("ADMIN")
        // ...
    );
}
```

### 13.9 JwtFilter 수정 (쿠키 지원)

```java
// JwtFilter.java - 쿠키에서도 토큰 읽기
@Override
protected void doFilterInternal(HttpServletRequest request, ...) {
    String token = null;

    // 1. Authorization 헤더에서 토큰 확인 (기존 API용)
    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        token = authHeader.substring(7);
    }

    // 2. 쿠키에서 토큰 확인 (어드민 페이지용)
    if (token == null && request.getCookies() != null) {
        token = Arrays.stream(request.getCookies())
            .filter(c -> "admin_token".equals(c.getName()))
            .findFirst()
            .map(Cookie::getValue)
            .orElse(null);
    }

    if (token == null) {
        filterChain.doFilter(request, response);
        return;
    }

    // 이하 기존 토큰 검증 로직...
}
```

### 13.10 로그인 실패 시 리다이렉트

```java
// 인증 실패 시 로그인 페이지로 리다이렉트 (어드민 페이지 전용)
// CustomAuthenticationEntryPoint 또는 ExceptionHandler에서 처리

@Component
public class AdminAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, ...) {
        String requestUri = request.getRequestURI();

        if (requestUri.startsWith("/admin")) {
            // 어드민 페이지 → 로그인 페이지로 리다이렉트
            response.sendRedirect("/admin/login");
        } else {
            // API → 401 응답
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
    }
}
```

### 13.10 UI 와이어프레임 (SB Admin 2 기반)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ ≡ OneTime Admin                                 2025-01-26    Admin ▼       │
├───────────────┬─────────────────────────────────────────────────────────────┤
│               │                                                             │
│  📊 대시보드    │  대시보드                        데이터 기준: 2025-01-26    │
│  ─────────    │                                                             │
│  통계         │  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐   │
│  ├ 👥 유저    │  │ ▎총 유저   │ │ ▎총 이벤트 │ │ ▎MAU      │ │ ▎휴면율   │   │
│  ├ 📅 이벤트  │  │  2,115    │ │  2,735    │ │   492     │ │  76.46%  │   │
│  └ 📈 리텐션  │  │     👥    │ │     📅    │ │    📈    │ │    🛏    │   │
│               │  └───────────┘ └───────────┘ └───────────┘ └───────────┘   │
│  마케팅       │                                                             │
│  └ 📢 타겟    │  ┌────────────────────────────────┐ ┌──────────────────┐   │
│               │  │  월별 신규 가입 유저             │ │ OAuth 제공자 비율 │   │
│  ─────────    │  │                                │ │                  │   │
│  [◀ Toggle]   │  │       ╭─╮                      │ │    ┌─────┐      │   │
│               │  │    ╭──╯ ╰──╮      ╭──╮        │ │   ╱  G    ╲     │   │
│               │  │ ╭──╯       ╰──────╯  ╰─       │ │  │   45%   │    │   │
│               │  │─╯                             │ │   ╲  K N  ╱     │   │
│               │  │ Jan Feb Mar Apr May Jun       │ │    ╲35 20╱      │   │
│               │  └────────────────────────────────┘ └──────────────────┘   │
│               │                                                             │
│               │  ┌────────────────────────────────┐ ┌──────────────────┐   │
│               │  │  요일별 이벤트 생성              │ │ 키워드 TOP 10    │   │
│               │  │                                │ │                  │   │
│               │  │  █████████████  목 (최다)       │ │ 1. 회의    825  │   │
│               │  │  ███████████    수             │ │ 2. 술       51  │   │
│               │  │  ██████████     화             │ │ 3. 스터디   50  │   │
│               │  │  █████████      월             │ │ 4. 밥       45  │   │
│               │  │  ████████       금             │ │ 5. MT       32  │   │
│               │  └────────────────────────────────┘ └──────────────────┘   │
│               │                                                             │
├───────────────┴─────────────────────────────────────────────────────────────┤
│                    Copyright © OneTime Admin 2025                           │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 13.11 AdminLTE 이관 가이드

> **SB Admin 2 → AdminLTE 이관 시 변경 사항**

| 항목 | SB Admin 2 | AdminLTE | 작업량 |
|------|-----------|----------|--------|
| Bootstrap | 4.6 | 5.x | 클래스 일부 변경 |
| 사이드바 | `.sidebar` | `.main-sidebar` | Fragment 교체 |
| 카드 | `.card.border-left-*` | `.card.card-*` | CSS 클래스 변경 |
| 차트 | Chart.js (동일) | Chart.js (동일) | **변경 없음** |
| 아이콘 | Font Awesome 5 | Font Awesome 5/6 | 호환 |

**이관 절차:**
1. AdminLTE dist 폴더 `static/admin/`에 복사
2. `fragments/` 폴더 4개 파일 교체
3. 카드/버튼 CSS 클래스 변경 (검색-치환)
4. 테스트

**예상 소요 시간: 3-4시간**

---

## 14. 구현 순서

### Step 1: 프로젝트 설정 (30분)
```bash
# 1. 의존성 추가 (build.gradle)
implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
implementation 'nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.3.0'

# 2. SB Admin 2 다운로드 및 복사
# https://startbootstrap.com/theme/sb-admin-2
# → src/main/resources/static/admin/ 폴더로 복사

# 3. 디렉토리 구조 생성
mkdir -p src/main/resources/templates/admin/{fragments,layout}
mkdir -p src/main/resources/static/admin/css
```

### Step 2: Fragment + 레이아웃 구현 (1시간)
1. `fragments/header.html` - CSS 로드
2. `fragments/sidebar.html` - 좌측 네비게이션
3. `fragments/topbar.html` - 상단바
4. `fragments/footer.html` - 푸터 + JS 로드
5. `layout/default.html` - Fragment 조합

### Step 3: 인증 처리 (1시간)
1. `AdminPageController` 생성 (로그인/로그아웃)
2. `login.html` 페이지 구현
3. `SecurityConfig`에 `/admin/**` URL 설정
4. `JwtFilter`에 쿠키 토큰 읽기 로직 추가
5. `AdminAuthenticationEntryPoint` 리다이렉트 처리

### Step 4: 통계 서비스 구현 (2-3시간)
1. `StatisticsService` 생성
2. 핵심 통계 쿼리 구현
   - 월별 가입자 수
   - OAuth 제공자 분포
   - MAU / 휴면율
   - 요일별 이벤트 분포
   - 키워드 TOP 10
3. Chart.js 호환 DTO 변환

### Step 5: 대시보드 페이지 (2시간)
1. `dashboard.html` - 요약 카드 + 차트 4개
2. Chart.js 초기화 스크립트
3. Thymeleaf → JavaScript 데이터 전달

### Step 6: 상세 페이지 (각 1시간)
1. `users.html` - 유저 통계 상세
2. `events.html` - 이벤트 통계 상세
3. `retention.html` - 리텐션 분석
4. `marketing.html` - 마케팅 타겟 리스트

### Step 7: 테스트 및 마무리 (1시간)
1. 로그인/로그아웃 플로우 테스트
2. 각 페이지 차트 렌더링 확인
3. 반응형 레이아웃 확인

**총 예상 소요: 8-10시간**
