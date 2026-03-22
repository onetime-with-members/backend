# Admin Banner Management Page Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 기존 React 어드민의 배너 관리 기능을 백엔드 Thymeleaf 어드민 페이지로 통합한다.

**Architecture:** 기존 REST API(`BannerController` 14개 엔드포인트)는 변경 없이 그대로 사용. Thymeleaf 페이지에서 Vanilla JS `fetch()`로 호출. 기존 이메일 페이지(`email.html`)의 탭/모달/페이징 패턴을 그대로 답습.

**Tech Stack:** Thymeleaf 3.1, Tailwind CSS (CDN), Lucide Icons, Vanilla JS, 기존 modal/alert 시스템 재사용

---

## Task 1: Sidebar + Controller 수정

**Files:**
- Modify: `src/main/resources/templates/admin/fragments/sidebar.html:57-67`
- Modify: `src/main/java/side/onetime/controller/AdminPageController.java:224-236`

**Step 1: sidebar.html - 도구 섹션에 "배너 관리" 메뉴 추가**

이메일 발송 링크 바로 아래에 추가:

```html
<!-- 기존 이메일 발송 링크 아래에 추가 -->
<a th:href="@{/admin/banner}"
   th:classappend="${currentPage == 'banner'} ? 'bg-accent-50 dark:bg-accent-900/30 text-accent-700 dark:text-accent-300' : 'text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-800 hover:text-gray-900 dark:hover:text-white'"
   class="flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors">
    <i data-lucide="image" class="w-4 h-4"></i>
    <span>배너 관리</span>
</a>
```

**Step 2: AdminPageController - 배너 페이지 라우트 추가**

이메일 페이지 메서드 패턴과 동일하게 추가:

```java
// ==================== Banner Page ====================

/**
 * 배너 관리 페이지 렌더링
 */
@GetMapping("/banner")
public String bannerPage(HttpServletRequest request, Model model) {
    model.addAttribute("currentUri", request.getRequestURI());
    model.addAttribute("currentPage", "banner");
    model.addAttribute("pageTitle", "Banner Management");
    return "admin/banner";
}
```

---

## Task 2: 배너 관리 페이지 HTML 작성 - 구조 + 캐러셀 배너 탭

**Files:**
- Create: `src/main/resources/templates/admin/banner.html`

**페이지 구조:**
- Thymeleaf layout:decorate로 `admin/layout/default` 상속
- 2개 탭: 캐러셀 배너 / 띠배너
- 각 탭에 요약 카드 + 테이블 + 페이징 + CRUD 모달

**레이아웃:**
```
layout:decorate="~{admin/layout/default}"
├── layout:fragment="content"
│   ├── Header (제목 + 설명)
│   ├── Tabs (캐러셀 배너 | 띠배너)
│   ├── Tab 1: 캐러셀 배너
│   │   ├── 요약 카드 (전체/활성/비활성/총 클릭)
│   │   └── 테이블 + 페이징
│   ├── Tab 2: 띠배너
│   │   ├── 요약 카드 (전체/활성/비활성)
│   │   └── 테이블 + 페이징
│   ├── 배너 등록/수정 모달 (이미지 업로드 + 미리보기)
│   └── 띠배너 등록/수정 모달 (색상 미리보기)
└── layout:fragment="scripts"
    └── JS (탭전환, CRUD, 페이징, 미리보기)
```

**API 호출 규약 (snake_case JSON):**

배너 API 응답 필드:
```json
{
  "is_success": true,
  "payload": {
    "banners": [
      {
        "id": 1,
        "organization": "...",
        "title": "...",
        "sub_title": "...",
        "button_text": "...",
        "color_code": "#677CEE",
        "image_url": "https://...",
        "is_activated": false,
        "created_date": "2026-02-12 10:30:00",
        "link_url": "https://...",
        "click_count": 42
      }
    ],
    "page_info": { "page": 1, "size": 20, "total_elements": 5, "total_pages": 1 }
  }
}
```

띠배너 API 응답 필드:
```json
{
  "is_success": true,
  "payload": {
    "bar_banners": [
      {
        "id": 1,
        "content_kor": "...",
        "content_eng": "...",
        "background_color_code": "#FFD700",
        "text_color_code": "#FFFFFF",
        "is_activated": true,
        "created_date": "2026-02-12 10:30:00",
        "link_url": "https://..."
      }
    ],
    "page_info": { ... }
  }
}
```

**배너 등록 (multipart/form-data):**
```javascript
const formData = new FormData();
formData.append('request', new Blob([JSON.stringify({
    organization, title, sub_title, button_text, color_code, link_url
})], { type: 'application/json' }));
formData.append('image_file', fileInput.files[0]);
fetch('/api/v1/banners/register', { method: 'POST', body: formData });
```

**배너 수정 (multipart/form-data):**
```javascript
const formData = new FormData();
formData.append('request', new Blob([JSON.stringify({
    organization, title, sub_title, button_text, color_code, link_url, is_activated
})], { type: 'application/json' }));
if (newImage) formData.append('image_file', fileInput.files[0]);
fetch(`/api/v1/banners/${id}`, { method: 'PATCH', body: formData });
```

**배너 활성 토글 (multipart/form-data):**
```javascript
const formData = new FormData();
formData.append('request', new Blob([JSON.stringify({
    is_activated: !currentState
})], { type: 'application/json' }));
fetch(`/api/v1/banners/${id}`, { method: 'PATCH', body: formData });
```

**띠배너 등록/수정/토글 (JSON):**
```javascript
fetch('/api/v1/bar-banners/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ content_kor, content_eng, background_color_code, text_color_code, link_url })
});
```

**UI 패턴 (email.html에서 답습):**
- 탭 전환: `.tab-btn` + `.tab-content` 클래스 토글
- 테이블: `<thead>` 고정 + `<tbody id="...">` JS 렌더링
- 페이징: `renderPagination(data)` 함수 패턴
- 모달: `fixed inset-0 bg-black/50 z-50 flex items-center justify-center` 패턴
- 알림: `showAlert(msg, type)`, `showConfirm(msg, title)` 재사용
- 아이콘 갱신: DOM 변경 후 `lucide.createIcons()` 호출

**캐러셀 배너 모달 필드:**
| 필드 | ID | 타입 | 필수 |
|------|-----|------|------|
| 조직명 | bannerOrganization | text | Y |
| 제목 | bannerTitle | text | Y |
| 부제목 | bannerSubTitle | text | Y |
| 버튼 텍스트 | bannerButtonText | text | Y |
| 색상 코드 | bannerColorCode | color + text | Y |
| 링크 URL | bannerLinkUrl | text | N |
| 이미지 | bannerImageFile | file | 등록 시 Y |

**띠배너 모달 필드:**
| 필드 | ID | 타입 | 필수 |
|------|-----|------|------|
| 내용(한) | barContentKor | text | Y |
| 내용(영) | barContentEng | text | Y |
| 배경색 | barBgColor | color + text | Y |
| 텍스트색 | barTextColor | color + text | Y |
| 링크 URL | barLinkUrl | text | N |

**미리보기 구현:**
- 배너: 이미지 FileReader + 텍스트 오버레이, colorCode 실시간 반영
- 띠배너: 56px 높이 바, background-color + color inline style

---

## Task 3: 빌드 검증

**Step 1: 빌드 확인**

```bash
cd /Users/hansangho/Desktop/onetime/backend
./gradlew build -x test
```

Expected: BUILD SUCCESSFUL

---

## Implementation Order

1. Task 1 (sidebar + controller) — 독립적, 바로 시작
2. Task 2 (banner.html 전체) — Task 1과 병렬 가능
3. Task 3 (빌드 검증) — Task 1, 2 완료 후
