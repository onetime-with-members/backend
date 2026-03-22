# 어드민 배너 관리 페이지 설계

## 1. 개요

### 1.1 목적
- 기존 React/Next.js 어드민에서 관리하던 배너(캐러셀 배너 + 띠배너)를 백엔드 Thymeleaf 어드민 페이지로 통합
- 기존 백엔드 REST API(`BannerController`)를 그대로 활용, UI 페이지만 신규 추가

### 1.2 현재 상태
| 구분 | 현재 | 목표 |
|------|------|------|
| 배너 API | `BannerController` 완성 (CRUD 14개 엔드포인트) | 변경 없음 |
| 배너 서비스 | `BannerService` 완성 (S3 이미지 관리 포함) | 변경 없음 |
| 어드민 UI | 없음 (API만 존재) | Thymeleaf 페이지 추가 |
| 프론트엔드 어드민 | React에서 API 직접 호출 | 제거 대상 (백엔드로 통합) |

### 1.3 기술 스택
- 기존 어드민 페이지와 동일: Thymeleaf + Tailwind CSS + Lucide Icons + Vanilla JS
- 이미지 업로드: `multipart/form-data` (기존 API 활용)

---

## 2. 기존 시스템 분석

### 2.1 배너 종류

#### 캐러셀 배너 (Banner)
- **용도**: 이벤트 상세 페이지에 자동 회전하는 마케팅/프로모션 배너
- **필드**: organization, title, subTitle, buttonText, colorCode(hex), imageUrl, linkUrl, clickCount, isActivated
- **이미지**: S3 업로드 (`banner/{bannerId}/`)
- **표시**: 3초 간격 자동 회전 캐러셀, 클릭 시 링크 이동 + 클릭 카운트 증가

#### 띠배너 (BarBanner)
- **용도**: 페이지 상단 알림/공지 바
- **필드**: contentKor, contentEng, backgroundColorCode(hex), textColorCode(hex), linkUrl, isActivated
- **이미지**: 없음 (텍스트 전용)
- **표시**: 상단 고정 56px, 쿠키 기반 24시간 닫기, 한/영 다국어

### 2.2 기존 API 엔드포인트 (변경 없음)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/v1/banners/register` | 배너 등록 (multipart) |
| GET | `/api/v1/banners/{id}` | 배너 단건 조회 |
| GET | `/api/v1/banners/all?page=1` | 배너 전체 조회 (페이징) |
| GET | `/api/v1/banners/activated/all` | 활성 배너 조회 |
| PATCH | `/api/v1/banners/{id}` | 배너 수정 (multipart) |
| DELETE | `/api/v1/banners/{id}` | 배너 삭제 (논리) |
| PATCH | `/api/v1/banners/{id}/clicks` | 클릭 수 증가 |
| POST | `/api/v1/bar-banners/register` | 띠배너 등록 (JSON) |
| GET | `/api/v1/bar-banners/{id}` | 띠배너 단건 조회 |
| GET | `/api/v1/bar-banners/all?page=1` | 띠배너 전체 조회 (페이징) |
| GET | `/api/v1/bar-banners/activated/all` | 활성 띠배너 조회 |
| PATCH | `/api/v1/bar-banners/{id}` | 띠배너 수정 (JSON) |
| DELETE | `/api/v1/bar-banners/{id}` | 띠배너 삭제 (논리) |

---

## 3. 구현 범위

### 3.1 변경/추가 파일

| 파일 | 작업 | 설명 |
|------|------|------|
| `AdminPageController.java` | 수정 | `/admin/banner` 라우트 추가 |
| `templates/admin/banner.html` | **신규** | 배너 관리 페이지 템플릿 |
| `templates/admin/fragments/sidebar.html` | 수정 | 사이드바에 "배너 관리" 메뉴 추가 |

### 3.2 백엔드 변경 없음
- `BannerController`, `BannerService`, 엔티티, DTO, 리포지토리 모두 기존 그대로 사용
- 어드민 페이지에서 JS `fetch()`로 기존 REST API를 호출하는 구조

---

## 4. UI 설계

### 4.1 페이지 구조

```
/admin/banner
├── 탭: [캐러셀 배너] [띠배너]
│
├── 캐러셀 배너 탭
│   ├── 요약 카드 (전체 / 활성 / 비활성 / 총 클릭 수)
│   ├── [+ 배너 등록] 버튼
│   └── 배너 목록 테이블
│       ├── 이미지 썸네일 | 제목 | 조직 | 상태 | 클릭 수 | 등록일 | 액션
│       └── 페이징
│
└── 띠배너 탭
    ├── 요약 카드 (전체 / 활성 / 비활성)
    ├── [+ 띠배너 등록] 버튼
    └── 띠배너 목록 테이블
        ├── 내용(한) | 내용(영) | 배경색 미리보기 | 상태 | 등록일 | 액션
        └── 페이징
```

### 4.2 캐러셀 배너 탭 상세

#### 목록 테이블 컬럼

| 컬럼 | 설명 |
|------|------|
| 이미지 | 40x40 썸네일 (rounded) |
| 제목 | title (truncate, 클릭 시 상세 모달) |
| 조직 | organization |
| 상태 | 활성(초록 badge) / 비활성(회색 badge) |
| 클릭 수 | clickCount (tabular-nums) |
| 등록일 | createdDate (yyyy-MM-dd) |
| 액션 | 활성 토글 / 수정 / 삭제 버튼 |

#### 등록/수정 모달

```
┌─────────────────────────────────────┐
│ 배너 등록 (또는 수정)                   │
├─────────────────────────────────────┤
│ 조직명*:     [________________]     │
│ 제목*:       [________________]     │
│ 부제목*:     [________________]     │
│ 버튼 텍스트*: [________________]     │
│ 색상 코드*:  [#___] [미리보기■]      │
│ 링크 URL:    [________________]     │
│ 이미지*:     [파일 선택] [미리보기]    │
│                                     │
│ 미리보기 영역:                        │
│ ┌─────────────────────────────────┐ │
│ │  (배너 미리보기 카드)              │ │
│ └─────────────────────────────────┘ │
│                                     │
│            [취소]  [등록/저장]        │
└─────────────────────────────────────┘
```

- 색상 코드 입력 시 실시간 미리보기 반영
- 이미지 선택 시 즉시 미리보기 표시 (FileReader)
- 수정 시 기존 값 자동 채움, 이미지 변경은 선택 사항

#### 배너 미리보기 카드
- 프론트엔드의 `Banner` 컴포넌트와 유사한 레이아웃 재현
- 배경 이미지 + 오버레이 텍스트(organization, title, subTitle, buttonText)
- colorCode 적용하여 실제 표시와 동일하게 렌더링

### 4.3 띠배너 탭 상세

#### 목록 테이블 컬럼

| 컬럼 | 설명 |
|------|------|
| 내용(한) | contentKor (truncate) |
| 내용(영) | contentEng (truncate) |
| 색상 | 배경색+텍스트색 미리보기 바 (inline style) |
| 상태 | 활성(초록) / 비활성(회색) |
| 등록일 | createdDate |
| 액션 | 활성 토글 / 수정 / 삭제 |

#### 등록/수정 모달

```
┌─────────────────────────────────────┐
│ 띠배너 등록 (또는 수정)                │
├─────────────────────────────────────┤
│ 내용(한)*:   [________________]     │
│ 내용(영)*:   [________________]     │
│ 배경색*:     [#___] [미리보기■]      │
│ 텍스트색*:   [#___] [미리보기■]      │
│ 링크 URL:    [________________]     │
│                                     │
│ 미리보기:                             │
│ ┌─────────────────────────────────┐ │
│ │ 🔊 내용(한) 텍스트 미리보기        │ │
│ └─────────────────────────────────┘ │
│                                     │
│            [취소]  [등록/저장]        │
└─────────────────────────────────────┘
```

- 배경색/텍스트색 변경 시 미리보기 바 실시간 반영
- 실제 띠배너 형태(56px 높이, 아이콘 + 텍스트)로 미리보기

### 4.4 공통 UI 패턴 (기존 어드민 스타일 준수)

| 패턴 | 적용 |
|------|------|
| 카드 | `bg-white dark:bg-gray-800 border rounded-lg` |
| 테이블 | 줄무늬, 호버, 반응형 |
| 모달 | `showAlert()`, `showConfirm()` 재사용 |
| 상태 뱃지 | 초록(활성), 회색(비활성) |
| 버튼 | Primary(accent-500), Secondary(gray border), Danger(red) |
| 토스트 | 기존 알림 시스템 재사용 |
| 다크 모드 | `dark:` prefix 전체 적용 |
| 스켈레톤 | 로딩 시 기존 스켈레톤 패턴 |

---

## 5. 구현 단계

### Step 1: 사이드바 메뉴 추가
- `fragments/sidebar.html`에 "배너 관리" 항목 추가
- 도구 섹션 하위, `image` 아이콘 사용
- `currentPage == 'banner'` 활성 상태 처리

### Step 2: AdminPageController 라우트 추가
- `GET /admin/banner` → `admin/banner` 템플릿 렌더링
- `model.addAttribute("currentPage", "banner")`

### Step 3: 배너 관리 페이지 작성 (`banner.html`)
- **탭 UI**: 캐러셀 배너 / 띠배너 전환
- **캐러셀 배너 목록**: API `GET /api/v1/banners/all` 호출, 테이블 렌더링, 페이징
- **띠배너 목록**: API `GET /api/v1/bar-banners/all` 호출, 테이블 렌더링, 페이징
- **요약 카드**: 전체/활성/비활성 카운트 표시

### Step 4: CRUD 모달 구현
- **배너 등록**: `multipart/form-data` 형식 `POST /api/v1/banners/register`
- **배너 수정**: `multipart/form-data` 형식 `PATCH /api/v1/banners/{id}`
- **띠배너 등록**: JSON `POST /api/v1/bar-banners/register`
- **띠배너 수정**: JSON `PATCH /api/v1/bar-banners/{id}`
- **삭제**: `showConfirm()` 확인 후 `DELETE` 호출
- **활성 토글**: `PATCH` API로 `isActivated` 값만 변경

### Step 5: 미리보기 기능
- 배너: 이미지 + 오버레이 텍스트 실시간 미리보기
- 띠배너: 배경색 + 텍스트색으로 실제 바 형태 미리보기

---

## 6. JS → API 호출 매핑

```javascript
// === 캐러셀 배너 ===

// 목록 조회
GET /api/v1/banners/all?page={page}

// 등록 (FormData)
POST /api/v1/banners/register
Content-Type: multipart/form-data
- request: JSON blob (RegisterBannerRequest)
- image_file: File

// 수정 (FormData)
PATCH /api/v1/banners/{id}
Content-Type: multipart/form-data
- request: JSON blob (UpdateBannerRequest)
- image_file: File (optional)

// 삭제
DELETE /api/v1/banners/{id}

// 활성 토글 (수정 API 재활용)
PATCH /api/v1/banners/{id}
- request: { is_activated: true/false }

// === 띠배너 ===

// 목록 조회
GET /api/v1/bar-banners/all?page={page}

// 등록
POST /api/v1/bar-banners/register
Content-Type: application/json

// 수정
PATCH /api/v1/bar-banners/{id}
Content-Type: application/json

// 삭제
DELETE /api/v1/bar-banners/{id}

// 활성 토글 (수정 API 재활용)
PATCH /api/v1/bar-banners/{id}
Content-Type: application/json
{ "is_activated": true/false }
```

### 6.1 multipart 요청 구성 (배너 등록/수정)

```javascript
const formData = new FormData();
const requestBlob = new Blob([JSON.stringify({
    organization: '...',
    title: '...',
    sub_title: '...',
    button_text: '...',
    color_code: '#677CEE',
    link_url: 'https://...'
})], { type: 'application/json' });

formData.append('request', requestBlob);
formData.append('image_file', fileInput.files[0]);

fetch('/api/v1/banners/register', {
    method: 'POST',
    body: formData
    // Content-Type은 자동 설정 (boundary 포함)
});
```

### 6.2 활성 토글 주의사항
- 배너 수정 API가 `multipart/form-data`를 요구하므로, 활성 토글 시에도 FormData 형식 필요
- `request` 부분에 `{ "is_activated": true/false }`만 담고, `image_file`은 생략

---

## 7. 참고: 프론트엔드에서 제거 대상

통합 완료 후 React 프론트엔드에서 제거할 배너 관련 파일:

```
src/features/banner/           # 전체 디렉터리 (프론트 어드민용)
├── api/                       # 어드민 API 호출 (사용자용 fetch는 유지)
├── components/BarBanner/      # 사용자용 컴포넌트 (유지)
├── contexts/                  # 사용자용 컨텍스트 (유지)
├── constants/
└── types/
```

> 사용자 대면 컴포넌트(`BarBanner`, `BannerList`)와 관련 훅은 유지. 어드민 관리 기능만 백엔드로 이관.

---

## 8. 향후 확장 고려사항

- **배너 순서 관리**: 현재 `createdDate` 기준 정렬 → 추후 `displayOrder` 필드 추가 가능
- **배너 노출 기간**: `startDate`, `endDate` 필드 추가 시 자동 활성/비활성 처리 가능
- **클릭 분석**: 클릭 수 외 CTR(노출 대비 클릭률) 추적
- **A/B 테스트**: 복수 배너 활성 시 노출 비율 제어
