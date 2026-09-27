# 코디 · 플래너 서버 이전 설계 (백엔드 4단계)

- 작성일: 2026-09-27
- 브랜치: `feature/outfits-planner`
- 기준 명세: `deliverables/8반_김민_오늘뭐입지-API.yml`, `deliverables/8반_김민_오늘뭐입지-DB.dbml`
- 상위 계획: [docs/backend-plan.md](../../backend-plan.md) 4단계

## 1. 목표와 범위

코디와 주간 플래너를 localStorage에서 서버로 옮긴다. 3단계에서 옷장을 옮긴 것과 같은 방식으로, 백엔드 API를 구현하고 프론트엔드 코디 · 플래너 store를 API로 연결한다.

**성공 기준**

- 백엔드 API 7개가 명세대로 동작하고 통합 테스트가 통과한다.
- 코디 · 플래너를 쓰는 모든 화면이 브라우저에서 서버 데이터로 동작한다.
- 코디 목록과 주간 일정 조회의 SQL 실행 수가 코디 수에 비례해 늘지 않는다.

**범위 밖**

- 홈 요약(`GET /home/summary`), 오늘의 픽(`POST /home/today-pick`), 챌린지 라운드(`GET /challenge/rounds`)는 5단계.
- AI 호출(`/ai/**`)은 6단계. 그때까지 AI 추천 · 주간 추천 · 챌린지 평가 · 오늘의 픽 뽑기는 `src/lib/mockAi.js`가 후보를 만든다. 결과를 **저장하는 부분**만 이번 단계 API를 쓴다.

## 2. 결정 사항

| 항목 | 결정 | 이유 |
|---|---|---|
| 기존 로컬 코디 · 일정 (`today-outfit:outfits`, `today-outfit:schedules`) | 앱 시작 시 삭제 | 2단계 이전 데이터는 옛 옷 ID를 가리켜 표시할 수 없고, 이후 데이터는 테스트 · 데모용 몇 건뿐. 3단계 `clothes` 키 처리와 같은 방식 |
| 프론트엔드 데이터 방식 | 화면마다 필요한 데이터를 API로 받아온다 (옷장처럼 전체 캐시하지 않음) | 명세의 목록 API는 요약(썸네일 3벌)만 주고, 일정 API가 코디 요약을 포함한다. 전체 캐시는 코디마다 상세 호출(N+1)이 필요하거나 명세에 없는 필드를 추가해야 함 |
| 백엔드 조회 방식 | JPA 엔티티 + `hibernate.default_batch_fetch_size` 일괄 로딩 | 3단계 코드와 같은 스타일, 가장 단순. 쿼리 수는 테스트로 보장. 느려지면 목록 조회만 전용 쿼리로 교체 |
| 홈 "이걸로 입을래요" | 코디 생성(`source=RANDOM`) + 오늘 날짜 배치를 이번 단계 API로 | 저장은 4단계 API 범위 |
| 플래너 AI 주간 "전체 저장" | `POST /planner/weekly-outfits` 한 번 (한 트랜잭션) | 명세. 지금은 날짜마다 코디 생성 + 배치를 따로 호출 |
| 챌린지 결과 "코디 저장" | `POST /outfits` (`source=CHALLENGE`) | 저장은 4단계 API 범위 |

## 3. 백엔드

DB 테이블(`outfits`, `outfit_items`, `outfit_ai_tags`, `schedules`)은 1단계 V1 마이그레이션에 이미 있으므로 **새 마이그레이션은 없다.**

### 3.1 공통 응답 형태

- `ClothingSummary`: `id`, `name`, `category`, `color`, `image_url`
  - `image_url`은 3단계와 같이 저장소 키를 `ImageService.publicUrl()`로 바꾼 값
- `OutfitSummary`: `id`, `name`, `source`, `ai_score`, `thumbnails`(구성 순서 앞 3벌의 `ClothingSummary`), `created_at`
- `OutfitDetail`: `id`, `name`, `memo`, `source`, `request_text`, `ai_reason`, `ai_score`, `ai_comment`, `ai_tags`(문자열 배열), `items`(`item_order` 오름차순, 각 `{ item_order, clothing: ClothingSummary }`), `created_at`
- `Schedule`: `id`, `plan_date`, `outfit`(`OutfitSummary`), `updated_at`(최초 배치 시 null)

### 3.2 코디 API (`outfit` 패키지)

| API | 동작 |
|---|---|
| `GET /outfits?source=&page=&size=` | 내 코디 최신순(`created_at` 내림차순, 같으면 `id` 내림차순). `source` 필터 선택. 응답 `PagedResponse<OutfitSummary>` |
| `POST /outfits` → 201 | `clothing_ids` 배열 순서가 `item_order`(1부터). 응답 `OutfitDetail` |
| `GET /outfits/{outfitId}` | 응답 `OutfitDetail` |
| `DELETE /outfits/{outfitId}` → 204 | 배치된 일정은 DB `ON DELETE CASCADE`로 함께 삭제 |

**저장 요청 검증 (`OutfitCreateRequest`)**

| 필드 | 규칙 |
|---|---|
| `name` | 필수, 앞뒤 공백 제거 후 1~50자 |
| `memo` | 선택, 500자 이하 |
| `source` | 필수, `AI` / `MANUAL` / `RANDOM` / `CHALLENGE` |
| `clothing_ids` | 필수, 1개 이상 20개 이하, 중복 불가, 모두 로그인 사용자의 옷 |
| `request_text` | 선택, 500자 이하 |
| `ai_reason` | 선택 |
| `ai_score` | 선택, 0~100 |
| `ai_comment` | 선택, 500자 이하 |
| `ai_tags` | 선택, 최대 5개, 각 1~30자, 중복은 하나로 합침 |

- `clothing_ids` 상한 20은 명세에 없지만, 요청 하나로 과도한 행이 생기지 않게 둔다. 슬롯이 5개(상의 · 하의 · 신발 · 모자 · 액세서리)라 실제 사용 범위를 넉넉히 덮는다.
- **생성 경로와 맞지 않는 필드는 저장하지 않는다.** `request_text` · `ai_reason`은 `source=AI`일 때만, `ai_score` · `ai_comment` · `ai_tags`는 `source=CHALLENGE`일 때만 저장하고 그 외에는 무시한다.
- 소유권 확인: `clothing_ids`로 로그인 사용자의 옷을 조회해 개수가 다르면 400 `INVALID_CLOTHING`.
- 옷이 모두 삭제된 코디도 남는다(명세). 이때 `thumbnails`와 `items`는 빈 배열.

### 3.3 플래너 API (`planner` 패키지)

| API | 동작 |
|---|---|
| `GET /planner/schedules?start_date=&end_date=` | 두 날짜 포함 범위의 내 일정, `plan_date` 오름차순. 배치 없는 날짜는 목록에 없음 |
| `PUT /planner/schedules/{planDate}` `{ outfit_id }` | 없던 날짜 → 201, 있던 날짜 → 코디 교체 후 200. 응답 `Schedule` |
| `DELETE /planner/schedules/{planDate}` → 204 | 배치 해제 |
| `POST /planner/weekly-outfits` → 201 | 날짜별 AI 코디 생성 + 배치. 응답 `Schedule[]`(날짜 오름차순) |

- 기간 조회: `start_date`, `end_date` 필수. `start_date > end_date`이거나 두 날짜 포함 일수가 31일을 넘으면 400.
- 배치: `outfit_id`가 내 코디가 아니면 404. 같은 날짜에 동시 요청으로 `(user_id, plan_date)` 유니크 제약이 걸리면 교체로 한 번 다시 시도한다.
- 교체되어 일정에서 빠진 기존 코디는 삭제하지 않는다.
- 주간 저장 요청: `request_text` 필수(500자 이하), `days` 1~7개, 각 `{ plan_date, clothing_ids(1~20, 중복 불가), ai_reason(필수) }`, `plan_date` 중복 불가.
  - 날짜마다 코디 이름 `AI 주간 코디 {월}.{일}`(예: `AI 주간 코디 9.28`), `source=AI`, `request_text`, `ai_reason`으로 생성하고 그 날짜에 배치(있으면 교체).
  - 전체가 한 트랜잭션. 한 날짜라도 `INVALID_CLOTHING`이면 아무것도 저장하지 않는다.

### 3.4 조회 성능

- `application.yml`에 `spring.jpa.properties.hibernate.default_batch_fetch_size: 100`을 추가한다.
- 목록 · 일정 조회는 코디 → 구성 항목 → 옷을 지연 로딩하되, 일괄 로딩으로 코디 수와 무관하게 쿼리 수가 일정하게 한다.

## 4. 프론트엔드

### 4.1 store

- `stores/outfits.js`: 상태를 쌓지 않는 API 함수 모음.
  - `list({ source })`: `GET /outfits?size=100` (최대 100개. 페이지 순회는 필요해질 때 추가)
  - `get(id)`, `create(payload)`(응답 `OutfitDetail`), `remove(id)`
- `stores/planner.js`: 현재 보고 있는 기간의 일정만 날짜별로 보관.
  - `loadRange(start, end)`, `upsert(date, outfitId)` → `{ created: boolean, schedule }`, `remove(date)`, `saveWeekly({ requestText, days })`
- `lib/api.js`: 201/200 구분을 위해 응답 상태 코드를 함께 돌려주는 옵션(`{ withStatus: true }` → `{ status, data }`)을 추가한다.

### 4.2 화면

| 화면 | 변경 |
|---|---|
| `components/OutfitCard.vue` | 로컬 `clothingIds` + 옷장 조회 대신 서버 `thumbnails` 사용 |
| `OutfitListView` | 진입 시 `list()`, 불러오는 중 · 빈 목록 표시 |
| `OutfitDetailView` | `get(id)`, 구성 옷은 `items[].clothing`. 404면 토스트 후 목록으로. 삭제는 `remove(id)` |
| `OutfitAiCreateView`, `OutfitManualCreateView`, `ChallengeResultView` | 저장 시 `create()` 후 응답 `id`로 상세 이동. 실패 시 서버 메시지 표시 |
| `HomeView` | 오늘 코디는 `loadRange(오늘, 오늘)`. "이걸로 입을래요" → `create({ source: 'RANDOM', name: '오늘의 코디 {월}.{일}' })` 후 `upsert(오늘)` |
| `PlannerView` | 주 이동 시 `loadRange(주 시작, 주 끝)`. 배치 · 교체 · 해제 후 그 주를 다시 조회. 코디 선택 시트를 열 때 `list()`. 상세에서 온 배치 모드(`?placeOutfit=`)는 `get(id)`로 이름 표시. AI 주간 "전체 저장"은 `saveWeekly()` 한 번 |

### 4.3 정리

- `lib/seedDemo.js` 삭제, `LoginView`의 샘플 코디 생성 호출 제거. 데모 코디 · 일정은 서버 `DemoAccountSeeder`가 만든 것을 사용한다.
- `stores/auth.js`의 `removeLegacyLocalData()`에 `outfits`, `schedules` 키 추가.
- `stores/wardrobe.js`의 `removeClothingReference` 호출, `SettingsView`의 `outfits.purgeOwner` · `planner.purgeOwner` 호출 제거(서버가 처리).

## 5. 에러 처리

응답 형식은 기존 `{ code, error_code, message }`.

| 상황 | 응답 |
|---|---|
| 코디에 내 옷이 아니거나 없는 옷 포함 | 400 `INVALID_CLOTHING` "내 옷장에 없는 옷이 포함되어 있어요." (새 `ErrorCode`) |
| 입력값 오류(옷 ID 중복, 태그 개수 · 길이, 점수 범위 등) | 400 `INVALID_REQUEST` + 항목별 메시지 |
| 조회 기간 오류 | 400 `INVALID_REQUEST` "조회 기간은 최대 31일이에요." |
| 날짜 형식 오류 | 400 `INVALID_REQUEST` |
| 주간 저장 날짜 중복 · 개수 오류 | 400 `INVALID_REQUEST` |
| 없거나 남의 코디 (조회 · 삭제 · 배치 대상) | 404 `NOT_FOUND` "삭제되었거나 존재하지 않는 코디입니다." |
| 배치 없는 날짜 해제 | 404 `NOT_FOUND` "배치된 코디가 없는 날짜예요." |

프론트엔드는 저장 · 삭제 실패 시 서버 메시지를 토스트나 화면 에러 문구로 보여 준다. 상세 404는 안내 후 목록으로 이동한다. 401은 기존대로 `api.js`가 로그인 화면으로 보낸다.

## 6. 테스트

구현은 Superpowers `test-driven-development`(실패하는 테스트 먼저)로 진행한다.

**`OutfitApiTest`**
- 저장: `clothing_ids` 순서대로 `items`, `OutfitDetail` 형태
- 검증: 필드별 규칙, `INVALID_CLOTHING`(남의 옷, 없는 옷), 옷 ID 중복
- 생성 경로와 맞지 않는 필드 미저장(예: `MANUAL` + `ai_score`)
- 목록: 최신순, `source` 필터, 썸네일 = 구성 순서 앞 3벌, 페이지네이션
- 남의 코디 조회 · 삭제 404
- 코디 삭제 시 일정 삭제
- 옷이 모두 삭제된 코디 유지, 썸네일 빈 배열

**`ScheduleApiTest`**
- 기간 조회: 날짜 오름차순, 남의 일정 제외, 31일 제한, 시작일 > 종료일
- 배치: 신규 201, 교체 200(`updated_at` 채워짐), 남의 코디 404
- 해제: 204, 없으면 404, 날짜 형식 오류 400
- 주간 저장: 날짜별 AI 코디 생성(이름 · `request_text`), 기존 배치 교체, 하나라도 `INVALID_CLOTHING`이면 전체 취소, 날짜 중복 · 8일 이상 400

**쿼리 수**: 코디 5개와 20개일 때 목록 조회, 7일 일정 조회의 SQL 실행 수가 같은지 Hibernate 통계로 확인한다.

**데모**: 데모 로그인 후 `GET /outfits`에 코디 2개, 내일 날짜 일정 1건.

**브라우저 확인 (헤드리스 Chrome)**
- 직접 만들기 → 상세 → 목록 썸네일
- 상세에서 플래너 배치("배치했어요") → 같은 날 다시 배치("교체했어요") → 해제
- AI 주간 전체 저장 후 해당 주에 표시
- 홈 "이걸로 입을래요" → 오늘 코디 표시
- 코디 삭제 시 플래너에서도 사라짐
- 새로고침 후 유지, 데모 계정 코디 · 일정, 레거시 로컬 키 삭제
