# 코디 · 플래너 서버 이전 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 코디와 주간 플래너를 localStorage에서 서버 API(7개)로 옮기고, 프론트엔드 코디 · 플래너 화면을 그 API에 연결한다.

**Architecture:** 백엔드는 기존 `outfit` · `planner` 패키지에 Controller / Service / Requests / Responses를 추가하고, 연관 로딩은 JPA 엔티티 + `default_batch_fetch_size`로 처리한다. 프론트엔드는 코디를 캐시하지 않고 화면마다 API를 호출하며, 플래너 store만 현재 보고 있는 기간의 일정을 보관한다.

**Tech Stack:** Spring Boot 4.1.1, Java 21, Spring Data JPA, PostgreSQL 17(테스트는 Testcontainers), Vue 3.5, Pinia

**Spec:** [docs/superpowers/specs/2026-09-27-outfits-planner-design.md](../specs/2026-09-27-outfits-planner-design.md)

## Global Constraints

- JSON 필드는 snake_case(전역 Jackson 설정). 에러 응답은 `{ code, error_code, message }`.
- 남의 리소스 · 없는 리소스는 404, 메시지 "삭제되었거나 존재하지 않는 코디입니다."
- 내 옷이 아닌(또는 없는) 옷 포함 → 400 `INVALID_CLOTHING` "내 옷장에 없는 옷이 포함되어 있어요."
- 배치 없는 날짜 해제 → 404 "배치된 코디가 없는 날짜예요."
- 조회 기간 오류 → 400 `INVALID_REQUEST` "조회 기간은 최대 31일이에요."
- `clothing_ids`: 1~20개, 중복 불가. `ai_tags`: 최대 5개, 각 1~30자, 중복은 하나로 합침.
- `request_text` · `ai_reason`은 `source=AI`일 때만, `ai_score` · `ai_comment` · `ai_tags`는 `source=CHALLENGE`일 때만 저장.
- 주간 코디 이름: `AI 주간 코디 {월}.{일}` (0 채우지 않음, 예: `AI 주간 코디 9.28`). 홈 확정 코디 이름: `오늘의 코디 {월}.{일}`.
- 새 Flyway 마이그레이션 없음 (테이블은 V1에 있음).
- 백엔드 테스트는 `@IntegrationTest` + `@AutoConfigureMockMvc`, 기존 `ClothingApiTest`의 `signupAndLogin()` / `auth()` 도우미 패턴을 따른다.
- 커밋 메시지는 `<타입>: <제목>` (feat / test / docs / refactor), 끝에 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- 백엔드 테스트 실행: `backend/`에서 `./gradlew test` (Docker Desktop 필요).

## Review Focus

- 구성 옷 일부가 삭제된 코디: `item_order`에 빈 번호(1, 3)가 생겨도 `items`와 `thumbnails`는 남은 옷을 순서대로 보여야 한다. → Task 2 `listAndDetailSkipDeletedClothingKeepingOrder`
- 챌린지 AI 평가가 실패해 `ai_score` · `ai_comment` · `ai_tags`가 없는 `CHALLENGE` 저장은 성공하고 점수는 null이어야 한다. → Task 1 `challengeWithoutEvaluationIsSaved`
- 빈 문자열 · 공백만 있는 태그는 400이어야 한다(30자 제한만으로는 안 막힘). → Task 1 `blankTagIsRejected`
- 배치 요청 본문에 `outfit_id`가 없으면 404가 아니라 400이어야 한다. → Task 3 `upsertWithoutOutfitIdIsBadRequest`
- 주간 저장에서 월 · 일이 한 자리 · 두 자리인 날짜(9.8, 12.31)의 이름 형식. → Task 4 `weeklyNamesUseMonthDayWithoutPadding`

---

### Task 1: 코디 저장 · 상세 API

**Files:**
- Modify: `backend/src/main/java/com/todayoutfit/common/ErrorCode.java` (`INVALID_CLOTHING` 추가)
- Modify: `backend/src/main/java/com/todayoutfit/clothing/ClothingRepository.java`
- Create: `backend/src/main/java/com/todayoutfit/clothing/ClothingSummary.java`
- Create: `backend/src/main/java/com/todayoutfit/outfit/OutfitRequests.java`
- Create: `backend/src/main/java/com/todayoutfit/outfit/OutfitDetailResponse.java`
- Create: `backend/src/main/java/com/todayoutfit/outfit/OutfitService.java`
- Create: `backend/src/main/java/com/todayoutfit/outfit/OutfitController.java`
- Modify: `backend/src/main/java/com/todayoutfit/outfit/Outfit.java`
- Test: `backend/src/test/java/com/todayoutfit/outfit/OutfitApiTest.java`

**Interfaces:**
- Consumes: `ImageService.publicUrl(String key): String`, `ClothingRepository`, `UserRepository.getReferenceById`
- Produces:
  - `ErrorCode.INVALID_CLOTHING` (400, "내 옷장에 없는 옷이 포함되어 있어요.")
  - `ClothingRepository.findAllByUserIdAndIdIn(Long userId, Collection<Long> ids): List<Clothing>`
  - `record ClothingSummary(Long id, String name, ClothingCategory category, ClothingColor color, String imageUrl)` + `static ClothingSummary from(Clothing c, UnaryOperator<String> urlOf)`
  - `OutfitRequests.Create(String name, String memo, OutfitSource source, List<Long> clothingIds, String requestText, String aiReason, Integer aiScore, String aiComment, List<String> aiTags)`
  - `record OutfitDetailResponse(Long id, String name, String memo, OutfitSource source, String requestText, String aiReason, Integer aiScore, String aiComment, List<String> aiTags, List<Item> items, LocalDateTime createdAt)`, `record Item(int itemOrder, ClothingSummary clothing)`
  - `OutfitService.create(Long userId, OutfitRequests.Create req): OutfitDetailResponse`
  - `OutfitService.get(Long userId, Long outfitId): OutfitDetailResponse`
  - `OutfitService.newOutfit(User user, Long userId, String name, String memo, OutfitSource source, List<Long> clothingIds): Outfit` — 소유권 검증 + 항목 추가까지 한 저장 전 엔티티 (Task 4가 재사용)
  - `OutfitService.findOwned(Long userId, Long outfitId): Outfit` — 없으면 404 (Task 3이 재사용)
  - `Outfit.recordChallengeEvaluation(Integer aiScore, String aiComment, List<String> tags)`

- [ ] **Step 1: 실패하는 테스트 작성** — `OutfitApiTest`에 아래 테스트를 쓴다. 옷은 `POST /clothes`로 만든다.
  - `createReturnsDetailWithItemsInRequestOrder`: 옷 3벌 [c, a, b] 순서로 `MANUAL` 저장 → 201, `$.items[0].item_order`=1, `$.items[0].clothing.id`=c, `$.items[2].clothing.id`=b, `$.items[0].clothing` 에 `image_url` 필드 존재(사진 없으면 null), `$.source`="MANUAL", `$.ai_tags` 빈 배열
  - `getReturnsSameDetail`: 저장 후 `GET /outfits/{id}` → 200, 같은 `name` · `items` 길이
  - `aiFieldsSavedOnlyForAiSource`: `AI` + `request_text`/`ai_reason` → 둘 다 응답에 있음. `MANUAL` + `request_text`/`ai_score`=90 → `$.request_text` null, `$.ai_score` null
  - `challengeFieldsSavedOnlyForChallengeSource`: `CHALLENGE` + score 87, comment, tags ["미니멀","미니멀","오피스룩"] → `$.ai_score`=87, `$.ai_tags`=["미니멀","오피스룩"]
  - `challengeWithoutEvaluationIsSaved`: `CHALLENGE`, 평가 필드 없음 → 201, `$.ai_score` null, `$.ai_tags` 빈 배열
  - `othersOrMissingClothingIsInvalidClothing`: 다른 사용자 옷 id 포함 → 400 `INVALID_CLOTHING`, 없는 id 999999 포함 → 400 `INVALID_CLOTHING`
  - `validatesRequest`: 이름 공백 → 400 `INVALID_REQUEST`; `clothing_ids` [] → 400; 같은 id 두 번 → 400; 21개 → 400; `ai_score` 101 → 400; 태그 6개 → 400; 31자 태그 → 400
  - `blankTagIsRejected`: `CHALLENGE` + tags ["  "] → 400 `INVALID_REQUEST`
  - `othersOutfitIsNotFound`: 다른 사용자 코디 `GET` → 404, `$.message`="삭제되었거나 존재하지 않는 코디입니다."

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "com.todayoutfit.outfit.OutfitApiTest"`
Expected: FAIL (`/outfits` 매핑 없음 → 404, 컴파일 오류 시 해당 클래스 없음)

- [ ] **Step 3: 구현**
  - `OutfitRequests.Create`: 검증 애너테이션은 Global Constraints 값 그대로. `name` · `memo` · `request_text`는 compact constructor에서 trim. `clothingIds` 중복은 `@AssertTrue` 메서드(`isClothingIdsDistinct`)로 검사. 태그는 `List<@NotBlank @Size(max = 30) String>` + `@Size(max = 5)`.
  - `OutfitService.newOutfit`: `findAllByUserIdAndIdIn` 결과 수 ≠ `clothingIds` 수면 `ApiException(INVALID_CLOTHING)`. 요청 순서대로 `outfit.addItem(...)` (조회 결과를 id → Clothing 맵으로 바꿔 순서 유지).
  - `create`: `newOutfit` 후 source별로 `recordAiRecommendation` / `recordChallengeEvaluation` 호출, 태그는 `LinkedHashSet`으로 중복 제거, 저장 후 `OutfitDetailResponse` 변환.
  - `OutfitController`: `@RequestMapping("/outfits")`, `POST`(201), `GET /{outfitId}`.

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "com.todayoutfit.outfit.OutfitApiTest"`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add backend/src
git commit -m "feat: 코디 저장 · 상세 조회 API 구현"
```

---

### Task 2: 코디 목록 · 삭제 API와 일괄 로딩

**Files:**
- Modify: `backend/src/main/resources/application.yml` (`spring.jpa.properties.hibernate.default_batch_fetch_size: 100`)
- Modify: `backend/src/test/resources/application-test.yml` (`spring.jpa.properties.hibernate.generate_statistics: true`)
- Create: `backend/src/main/java/com/todayoutfit/outfit/OutfitSummaryResponse.java`
- Modify: `backend/src/main/java/com/todayoutfit/outfit/OutfitRepository.java`
- Modify: `backend/src/main/java/com/todayoutfit/outfit/OutfitService.java`, `OutfitController.java`
- Test: `backend/src/test/java/com/todayoutfit/outfit/OutfitApiTest.java`, Create `backend/src/test/java/com/todayoutfit/outfit/OutfitQueryCountTest.java`

**Interfaces:**
- Consumes: Task 1의 `ClothingSummary`, `OutfitService.findOwned`
- Produces:
  - `record OutfitSummaryResponse(Long id, String name, OutfitSource source, Integer aiScore, List<ClothingSummary> thumbnails, LocalDateTime createdAt)` + `static OutfitSummaryResponse from(Outfit o, UnaryOperator<String> urlOf)` — `thumbnails`는 `items` 앞 3개
  - `OutfitRepository.findAllByUserId(Long userId, Pageable p): Page<Outfit>`, `findAllByUserIdAndSource(Long userId, OutfitSource source, Pageable p): Page<Outfit>`, `findByIdAndUserId(Long id, Long userId): Optional<Outfit>`
  - `OutfitService.list(Long userId, OutfitSource source, Pageable p): PageResponse<OutfitSummaryResponse>` (정렬은 `createdAt` desc, `id` desc 고정)
  - `OutfitService.delete(Long userId, Long outfitId)`
  - `OutfitService.toSummary(Outfit o): OutfitSummaryResponse` (Task 3이 재사용)

- [ ] **Step 1: 실패하는 테스트 작성**
  - `OutfitApiTest.listIsNewestFirstWithSourceFilterAndThreeThumbnails`: 옷 4벌로 `MANUAL` 코디(4벌), 이어서 `AI` 코디 저장 → `GET /outfits` 첫 항목이 `AI` 코디; `MANUAL` 코디 `thumbnails` 길이 3, 첫 썸네일 id = 요청 첫 옷; `?source=AI` → `total_elements` 1; `?page=2&size=1` → `$.page`=2
  - `OutfitApiTest.deleteRemovesOutfitAndItsSchedules`: 코디 저장, DB에 `schedules` 행을 JdbcTemplate로 넣음 → `DELETE /outfits/{id}` 204 → `GET` 404, `select count(*) from schedules where outfit_id=?` = 0
  - `OutfitApiTest.othersOutfitCannotBeDeleted`: 다른 사용자 코디 `DELETE` → 404
  - `OutfitApiTest.listAndDetailSkipDeletedClothingKeepingOrder`: 옷 [a, b, c]로 코디 → `DELETE /clothes/{b}` → 상세 `items` 길이 2이고 `clothing.id` 순서 [a, c], 목록 `thumbnails` [a, c]; 옷 모두 삭제 → 목록 `thumbnails` 빈 배열, 코디는 목록에 남음
  - `OutfitQueryCountTest.listQueryCountDoesNotGrowWithOutfits`: 사용자 A에 코디 5개, B에 20개(각 3벌) 생성 → `SessionFactory.getStatistics().clear()` 후 `GET /outfits` 호출해 `getPrepareStatementCount()` 측정 → A와 B의 값이 같다

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "com.todayoutfit.outfit.*"`
Expected: FAIL (`GET /outfits` · `DELETE` 매핑 없음)

- [ ] **Step 3: 구현** — Interfaces의 시그니처대로. 목록은 `PageRequest.of(page, size, Sort.by(desc("createdAt"), desc("id")))`로 클라이언트 정렬 무시(옷장과 동일). 삭제는 `findOwned` 후 `outfitRepository.delete`.

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "com.todayoutfit.outfit.*"`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add backend/src
git commit -m "feat: 코디 목록 · 삭제 API 구현 및 연관 일괄 로딩 설정"
```

---

### Task 3: 플래너 기간 조회 · 배치 · 해제 API

**Files:**
- Modify: `backend/src/main/java/com/todayoutfit/planner/Schedule.java` (`changeOutfit`)
- Modify: `backend/src/main/java/com/todayoutfit/planner/ScheduleRepository.java`
- Create: `backend/src/main/java/com/todayoutfit/planner/ScheduleResponse.java`
- Create: `backend/src/main/java/com/todayoutfit/planner/PlannerRequests.java`
- Create: `backend/src/main/java/com/todayoutfit/planner/PlannerService.java`
- Create: `backend/src/main/java/com/todayoutfit/planner/PlannerController.java`
- Test: `backend/src/test/java/com/todayoutfit/planner/PlannerApiTest.java`

**Interfaces:**
- Consumes: `OutfitService.findOwned`, `OutfitService.toSummary`
- Produces:
  - `Schedule.changeOutfit(Outfit outfit)`
  - `ScheduleRepository.findAllByUserIdAndPlanDateBetweenOrderByPlanDateAsc(Long userId, LocalDate start, LocalDate end): List<Schedule>`, `findByUserIdAndPlanDate(Long userId, LocalDate date): Optional<Schedule>`
  - `record ScheduleResponse(Long id, LocalDate planDate, OutfitSummaryResponse outfit, LocalDateTime updatedAt)`
  - `PlannerRequests.Upsert(@NotNull(message = "배치할 코디를 선택해주세요.") Long outfitId)`
  - `record PlannerService.UpsertResult(boolean created, ScheduleResponse schedule)`
  - `PlannerService.list(Long userId, LocalDate start, LocalDate end): List<ScheduleResponse>`
  - `PlannerService.upsert(Long userId, LocalDate date, Long outfitId): UpsertResult`
  - `PlannerService.remove(Long userId, LocalDate date)`
  - `PlannerService.place(User user, LocalDate date, Outfit outfit): boolean created` — 없으면 새 `Schedule`, 있으면 `changeOutfit` (Task 4가 재사용)

- [ ] **Step 1: 실패하는 테스트 작성** (`PlannerApiTest`, 코디는 `POST /outfits`로 만든다)
  - `listReturnsRangeInDateOrderExcludingOthers`: 9/30, 9/28에 배치, 다른 사용자도 9/29에 배치 → `GET /planner/schedules?start_date=2026-09-28&end_date=2026-10-04` → 길이 2, `$[0].plan_date`="2026-09-28", `$[0].outfit.thumbnails` 존재, `$[0].updated_at` null
  - `listRejectsInvalidRange`: 31일 범위(9/1~10/1) → 200; 32일(9/1~10/2) → 400 `$.message`="조회 기간은 최대 31일이에요."; start > end → 400; `start_date` 누락 → 400
  - `upsertCreatesThenReplaces`: `PUT /planner/schedules/2026-09-28` {outfit A} → 201; {outfit B} → 200, `$.outfit.id`=B, `$.updated_at` not null
  - `upsertOthersOutfitIsNotFound`: 다른 사용자 코디 → 404
  - `upsertWithoutOutfitIdIsBadRequest`: 본문 `{}` → 400 `INVALID_REQUEST`
  - `upsertInvalidDateIsBadRequest`: `PUT /planner/schedules/2026-13-40` → 400
  - `removeUnschedules`: 배치 후 `DELETE` → 204, 다시 `DELETE` → 404 `$.message`="배치된 코디가 없는 날짜예요."

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "com.todayoutfit.planner.PlannerApiTest"`
Expected: FAIL

- [ ] **Step 3: 구현**
  - 기간 검증: `start.isAfter(end)` 또는 `ChronoUnit.DAYS.between(start, end) + 1 > 31` → `ApiException(INVALID_REQUEST, "조회 기간은 최대 31일이에요.")`.
  - 컨트롤러: `@RequestParam("start_date") @DateTimeFormat(iso = DATE) LocalDate`, 경로 `@PathVariable @DateTimeFormat(iso = DATE) LocalDate planDate`. `PUT`은 `ResponseEntity.status(created ? 201 : 200)`.
  - 동시 요청으로 `(user_id, plan_date)` 유니크 위반(`DataIntegrityViolationException`) 시 교체로 한 번 재시도: `upsert`를 트랜잭션 밖 진입점 + 내부 `@Transactional` 메서드(별도 빈 호출 또는 `TransactionTemplate`)로 나눈다.

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "com.todayoutfit.planner.PlannerApiTest"`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add backend/src
git commit -m "feat: 플래너 기간 조회 · 배치 · 해제 API 구현"
```

---

### Task 4: AI 주간 코디 일괄 저장 API

**Files:**
- Modify: `backend/src/main/java/com/todayoutfit/planner/PlannerRequests.java`, `PlannerService.java`, `PlannerController.java`
- Test: `backend/src/test/java/com/todayoutfit/planner/PlannerApiTest.java`, `backend/src/test/java/com/todayoutfit/planner/ScheduleQueryCountTest.java`, `backend/src/test/java/com/todayoutfit/auth/AuthApiTest.java`

**Interfaces:**
- Consumes: `OutfitService.newOutfit`, `PlannerService.place`
- Produces:
  - `PlannerRequests.Weekly(@NotBlank @Size(max = 500) String requestText, @NotEmpty @Size(max = 7) List<@Valid Day> days)` + `@AssertTrue isPlanDatesDistinct()`
  - `PlannerRequests.Day(@NotNull LocalDate planDate, @NotEmpty @Size(max = 20) List<Long> clothingIds, @NotBlank String aiReason)` + `@AssertTrue isClothingIdsDistinct()`
  - `PlannerService.saveWeekly(Long userId, PlannerRequests.Weekly req): List<ScheduleResponse>` (날짜 오름차순)
  - `POST /planner/weekly-outfits` → 201

- [ ] **Step 1: 실패하는 테스트 작성**
  - `PlannerApiTest.weeklyCreatesAiOutfitsAndSchedules`: 2일치(10/1, 10/2) → 201, 길이 2; `GET /outfits?source=AI` 2개, 상세 `request_text`="다음 주는 출근이 많아요", `ai_reason` 그대로
  - `PlannerApiTest.weeklyNamesUseMonthDayWithoutPadding`: 9/8, 12/31 → 코디 이름 "AI 주간 코디 9.8", "AI 주간 코디 12.31"
  - `PlannerApiTest.weeklyReplacesExistingSchedule`: 10/1에 코디 A 배치 → 주간 저장에 10/1 포함 → 10/1 일정의 `outfit.id` ≠ A, 코디 A는 `GET /outfits/{A}` 200(삭제 안 됨)
  - `PlannerApiTest.weeklyIsAllOrNothing`: 2일치 중 하나에 남의 옷 → 400 `INVALID_CLOTHING`, `GET /outfits?source=AI` `total_elements` 0, 해당 기간 일정 0건
  - `PlannerApiTest.weeklyValidatesDays`: `days` 8개 → 400; 같은 `plan_date` 두 번 → 400; `days` [] → 400
  - `ScheduleQueryCountTest.weekQueryCountDoesNotGrowWithSchedules`: 사용자 A는 2일, B는 7일 배치(각 코디 3벌) → 통계 초기화 후 7일 기간 조회 → `getPrepareStatementCount()` 같음
  - `AuthApiTest.demoLoginExposesSampleOutfitsAndTomorrowSchedule`: `/auth/demo` 토큰으로 `GET /outfits` `total_elements` 2, 내일~내일 일정 1건

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "com.todayoutfit.planner.*" --tests "com.todayoutfit.auth.AuthApiTest"`
Expected: FAIL (`/planner/weekly-outfits` 없음; 쿼리 수 · 데모 테스트는 이미 통과할 수 있음 — 통과하면 그대로 둔다)

- [ ] **Step 3: 구현** — `saveWeekly`는 한 `@Transactional`: 날짜마다 `newOutfit(..., source=AI)` + `recordAiRecommendation(requestText, aiReason)` + 저장 + `place`. 이름은 `"AI 주간 코디 " + date.getMonthValue() + "." + date.getDayOfMonth()`. 예외가 나면 전체 롤백.

- [ ] **Step 4: 전체 백엔드 테스트 통과 확인**

Run: `./gradlew test`
Expected: PASS (기존 53개 + 이번 단계 테스트 전부)

- [ ] **Step 5: 커밋**

```bash
git add backend/src
git commit -m "feat: AI 주간 코디 일괄 저장 API 구현"
```

---

### Task 5: 프론트엔드 API · store 기반 교체

프론트엔드에는 테스트 프레임워크가 없으므로 각 프론트 태스크의 검증은 `npm run build` 성공과 Task 8의 브라우저 확인이다.

**Files:**
- Modify: `src/lib/api.js`
- Rewrite: `src/stores/outfits.js`, `src/stores/planner.js`
- Modify: `src/stores/auth.js` (`removeLegacyLocalData`에 `outfits`, `schedules`)
- Modify: `src/stores/wardrobe.js` (`useOutfitsStore().removeClothingReference` 호출과 import 제거)
- Modify: `src/components/OutfitCard.vue` (`outfit.thumbnails` 사용, wardrobe store 의존 제거)
- Delete: `src/lib/seedDemo.js`; Modify: `src/views/LoginView.vue` (seed 호출 · import 제거, `outfits` · `planner` store import 제거)
- Modify: `src/views/SettingsView.vue` (`outfits.purgeOwner`, `planner.purgeOwner` 호출 · import 제거)

**Interfaces:**
- Produces:
  - `api.put(path, body, { withStatus: true })` → `{ status: number, data }` (옵션 없으면 기존처럼 data만)
  - `useOutfitsStore()`: `list({ source } = {}): Promise<OutfitSummary[]>` (`GET /outfits?size=100[&source=]`의 `content`), `get(id): Promise<OutfitDetail>`, `create(payload): Promise<OutfitDetail>` (payload는 camelCase: `name, memo, source, clothingIds, requestText, aiReason, aiScore, aiComment, aiTags`), `remove(id): Promise<void>`
  - `usePlannerStore()`: state `byDate: { [planDate]: Schedule }`; `loadRange(start, end): Promise<void>` (byDate를 그 기간 결과로 교체), `upsert(planDate, outfitId): Promise<{ created: boolean, schedule }>`, `remove(planDate): Promise<void>`, `saveWeekly({ requestText, days: [{ planDate, clothingIds, aiReason }] }): Promise<Schedule[]>`, getter `findByDate(planDate): Schedule | null`, `reset()`
  - `Schedule` 모양: `{ id, planDate, outfit: OutfitSummary, updatedAt }`, `OutfitSummary`: `{ id, name, source, aiScore, thumbnails, createdAt }`

- [ ] **Step 1:** 위 Interfaces대로 `api.js`, 두 store, `auth.js`, `wardrobe.js`, `OutfitCard.vue`, `LoginView.vue`, `SettingsView.vue`를 수정하고 `seedDemo.js`를 삭제한다. `App.vue`의 로그아웃 watcher에 `planner.reset()`을 추가한다.
- [ ] **Step 2: 빌드 확인**

Run: `npm run build`
Expected: `✓ built`. 이 시점에는 아직 옛 store API(`outfits.add`, `byOwner`, `planner.findByDate(userId, date)`)를 쓰는 화면이 있어 빌드는 통과해도 런타임은 Task 6 · 7에서 맞춘다.

- [ ] **Step 3: 커밋**

```bash
git add src
git commit -m "refactor: 코디 · 플래너 store를 서버 API 기반으로 교체"
```

---

### Task 6: 프론트엔드 코디 화면 연결

**Files:**
- Modify: `src/views/OutfitListView.vue`, `src/views/OutfitDetailView.vue`, `src/views/OutfitAiCreateView.vue`, `src/views/OutfitManualCreateView.vue`, `src/views/ChallengeResultView.vue`

**Interfaces:**
- Consumes: Task 5의 `useOutfitsStore()`

- [ ] **Step 1:** 목록 화면은 `onMounted`에서 `list()`, `loading` 동안 "불러오는 중", 빈 목록은 기존 `EmptyState`.
- [ ] **Step 2:** 상세 화면은 `props.id`로 `get()`. 구성 옷은 `outfit.items.map(i => i.clothing)`. `ApiError.status === 404`면 `show(e.message)` 후 `router.replace({ name: 'outfits' })`. 삭제는 `await remove(id)` 후 목록으로, 실패 시 `show(e.message)`.
- [ ] **Step 3:** AI · 직접 · 챌린지 저장은 `await outfits.create({...})`(기존 payload에서 `ownerId` 제거) 후 응답 `id`로 상세 이동. `saving`은 `finally`에서 해제, 실패 시 `show(e.message)`. 챌린지는 성공 시에만 `challenge.reset()`.
- [ ] **Step 4: 빌드 확인** — Run: `npm run build`, Expected: `✓ built`
- [ ] **Step 5: 커밋**

```bash
git add src
git commit -m "feat: 코디 목록 · 상세 · 저장 화면을 서버 API에 연결"
```

---

### Task 7: 프론트엔드 홈 · 플래너 연결

**Files:**
- Modify: `src/views/HomeView.vue`, `src/views/PlannerView.vue`

**Interfaces:**
- Consumes: Task 5의 `useOutfitsStore()`, `usePlannerStore()`

- [ ] **Step 1: 홈** — `onMounted`에서 `planner.loadRange(todayKey(), todayKey())`, 오늘 코디는 `planner.findByDate(todayKey())?.outfit`, 썸네일은 `outfit.thumbnails`. "이걸로 입을래요"는 `create({ name: '오늘의 코디 {월}.{일}', source: 'RANDOM', clothingIds })` → `upsert(todayKey(), created.id)` → 토스트 "오늘의 코디로 확정했어요". 실패 시 `show(e.message)`.
- [ ] **Step 2: 플래너 주간 표시** — `watch(weekStart, …, { immediate: true })`로 `loadRange(weekStart, addDays(weekStart, 6))`. `outfitFor(d)`는 `planner.findByDate(d)?.outfit`, 셀 썸네일은 `outfit.thumbnails[0]`.
- [ ] **Step 3: 배치 · 교체 · 해제** — `doPlaceOutfit` · `choosePickerOutfit`은 `upsert` 결과 `created`로 "코디를 배치했어요" / "코디를 교체했어요", `unscheduleFromSheet`은 `remove(date)` 후 "배치를 해제했어요". 각 작업 후 현재 주 `loadRange` 재호출. 코디 선택 시트를 열 때 `savedOutfits = await outfits.list()`. 배치 모드 배너는 `placeOutfitId`가 바뀔 때 `outfits.get(id)`로 이름 표시(404면 배치 모드 해제).
- [ ] **Step 4: AI 주간 전체 저장** — `planner.saveWeekly({ requestText: aiSituation.trim(), days: aiResults.map(d => ({ planDate: addDays(aiStartDate, d.dayOffset), clothingIds: d.clothingIds, aiReason: d.aiReason })) })` 한 번, 성공 토스트 후 현재 주 재조회. 실패 시 `aiError = e.message`.
- [ ] **Step 5: 빌드 확인** — Run: `npm run build`, Expected: `✓ built`; `grep -rn "outfits.add\|byOwner(\|clothingIds\[0\]\|purgeOwner\|seedDemo" src` 결과 없음(단, `wardrobe.byOwner`는 제외)
- [ ] **Step 6: 커밋**

```bash
git add src
git commit -m "feat: 홈 · 플래너 화면을 서버 API에 연결"
```

---

### Task 8: 브라우저 검증과 문서 반영

**Files:**
- Modify: `docs/backend-plan.md` (4단계 ✅, 결정 사항: 프론트 코디 데이터 방식 · 일괄 로딩 · 레거시 키 삭제, 남은 확인 사항 갱신)

- [ ] **Step 1: 서버 실행** — `backend/`에서 `docker compose up -d`, `PORT=9080 ./gradlew bootRun`, 루트에서 `BACKEND_URL=http://localhost:9080 npx vite --port 5199` (8080이 Windows 예약 범위일 수 있음)
- [ ] **Step 2: 헤드리스 Chrome 확인** — 새 사용자로 다음을 확인하고 모두 PASS여야 한다:
  - 레거시 `today-outfit:outfits` · `today-outfit:schedules` 키가 앱 시작 시 삭제
  - 옷 2벌 등록 → 직접 만들기 저장 → 상세에 2벌 → 목록 카드 썸네일 2개
  - 상세 "플래너에 배치" → 날짜 클릭 → "코디를 배치했어요", 같은 날짜에 다른 코디 → "코디를 교체했어요", 해제 → "배치를 해제했어요"
  - AI 주간 추천 → 전체 저장 → 해당 주 셀에 코디 표시
  - 홈 오늘의 픽 → "이걸로 입을래요" → 새로고침 후에도 오늘 코디 표시
  - 배치된 코디 삭제 → 플래너 해당 날짜 비어 있음
  - 데모 로그인 → 코디 목록 2개, 플래너 내일 칸에 코디
- [ ] **Step 3: 문서 반영 후 커밋**

```bash
git add docs/backend-plan.md
git commit -m "docs: 백엔드 4단계 완료 상태와 결정 사항 반영"
```
