# 백엔드 구축 계획

프론트엔드 전용(localStorage + AI 목업)으로 동작하던 "오늘 뭐 입지" 앱에 실제 백엔드를 붙이는 단계별 계획이다.
기준 명세는 `deliverables/8반_김민_오늘뭐입지-API.yml`(23개 경로)과 `deliverables/8반_김민_오늘뭐입지-DB.dbml`(7개 테이블)이다.

## 기술 스택

| 구분 | 선택 |
|---|---|
| 언어 · 프레임워크 | Java 21, Spring Boot 4.1.1 (Gradle wrapper) |
| DB | PostgreSQL 17 (Docker, `backend/compose.yaml`) |
| 스키마 관리 | Flyway (`backend/src/main/resources/db/migration`), JPA는 `ddl-auto: validate` |
| 인증 | Spring Security + BCrypt + JWT |
| API 경로 | `http://localhost:8080/api` (context-path `/api`), 프론트는 Vite 프록시로 연결 |

실행 방법은 [backend/README.md](../backend/README.md)를 참고한다.

## 진행 방식

- 한 번에 한 단계씩 진행하고, 단계가 끝날 때마다 결과를 확인한다.
- 2단계부터는 **기능 하나가 끝나면 해당 프론트엔드 store를 바로 API에 연결**한다. 단계마다 실제 앱 화면에서 동작을 확인하고, 명세와 프론트엔드가 어긋나는 부분을 일찍 찾기 위해서다.
- 실제 LLM 연동은 8단계 이후 별도로 진행한다. 6단계에서 AI 호출을 인터페이스로 분리해 두므로 구현체만 추가하면 된다.

## 단계

| 단계 | 내용 | 완료 기준 | 상태 |
|---|---|---|---|
| 0. 뼈대 | Spring 프로젝트 생성, Docker PostgreSQL, Vite `/api` 프록시 | 서버 기동, health 체크 응답 | ✅ 완료 |
| 1. 스키마 · 공통 규칙 | DBML → Flyway 마이그레이션, 엔티티 · enum, 공통 에러 응답, 페이지네이션 | 테이블 7개 생성, 에러 형식 통일 | ✅ 완료 |
| 2. 인증 | 회원가입 · 로그인 · 로그아웃 · 데모 계정 · 내 정보 · 탈퇴, BCrypt, JWT, 데모 샘플 데이터 서버 이전, 프론트 auth store 연결 | 토큰 없으면 401, 탈퇴 시 하위 데이터 삭제 | ✅ 완료 |
| 3. 옷장 · 이미지 | `/clothes` 조회(필터) · 등록 · 일괄 등록 · 상세 · 삭제, `/images` 업로드(형식 · 크기 검사) | 사진을 base64 대신 URL로 저장 | ⬜ |
| 4. 코디 · 플래너 | 코디 저장(순서 · 태그) · 목록 · 상세 · 삭제, 날짜별 배치 업서트(신규 201 / 교체 200), 주간 추천 일괄 저장 | 의류 삭제 시 코디에서 빠지고, 코디 삭제 시 일정 삭제 | ⬜ |
| 5. 홈 · 챌린지 | 홈 요약, 오늘의 픽, 챌린지 라운드 생성 | 프론트 계산 로직을 서버로 이전 | ⬜ |
| 6. AI 엔드포인트 (목업) | AI 호출 인터페이스 분리, `mockAi.js` 로직을 Java 목업으로 이전, `ai_requests` 기록, 일일 한도 초과 시 429 | `/ai/**` 5개 경로 동작 | ⬜ |
| 7. 프론트엔드 정리 | 공통 API 클라이언트 정리, 남은 localStorage 저장 코드 제거 | 앱 전체가 서버 데이터로 동작 | ⬜ |
| 8. 마무리 | 통합 테스트, Swagger UI로 명세 대조, README 정리 | 명세와 구현 차이 0 | ⬜ |

7단계는 원래 프론트엔드를 한꺼번에 연결하는 단계였지만, 기능별로 바로 연결하기로 하면서 남은 정리 작업만 하는 단계가 됐다.

## 결정 사항

| 항목 | 결정 | 이유 |
|---|---|---|
| DB | PostgreSQL | 무료 관리형 호스팅(Supabase, Neon)으로 배포 데모가 쉽고, 한글 설정이 필요 없으며, 나중에 pgvector로 추천 고도화 가능 |
| enum 저장 | PostgreSQL enum 타입 대신 `varchar` + CHECK 제약 | JPA 매핑이 단순하고, 값 추가 시 마이그레이션이 간단함 |
| 색상 값 | API · DB 모두 한글(`블랙` 등), 코드에서만 `BLACK` 등 영문 상수 | 명세의 `ClothingColor` enum을 그대로 따름 |
| 추가 에러 코드 | `INVALID_REQUEST`(400), `METHOD_NOT_ALLOWED`(405) | 명세에는 400 예시로 `INVALID_FILE`만 있어 일반 입력 오류용 코드가 필요함. 명세 반영 여부 미정 |
| 비밀값 관리 | `backend/.env`(커밋 제외) + `.env.example`(템플릿) | compose와 Spring Boot가 같은 파일을 읽고, 배포 시에는 환경변수로 대체 |
| JWT | Spring Security OAuth2 Resource Server(Nimbus) · HS256, subject = 사용자 ID | 별도 JWT 라이브러리 없이 서명 · 만료 검증을 표준 구현으로 처리 |
| 로그아웃 블랙리스트 | `revoked_tokens` 테이블(V2)에 jti와 만료 시각 저장, 1시간마다 만료분 삭제 | 명세의 "블랙리스트 등록"을 서버 재시작 후에도 유지. DBML에는 없는 테이블 |
| 데모 계정 | `demo@today-outfit.app` 하나를 모든 체험자가 공유, 일반 가입에서는 이 이메일을 예약 | 명세 "계정이 없으면 생성". 누군가 탈퇴하면 다음 체험 때 새로 생성됨 |
| 추가 에러 코드 | `PASSWORD_MISMATCH`, `INVALID_PASSWORD`(400) | 명세 본문 설명에 있는 코드를 그대로 사용 |
| 시간대 | 서버 기본 시간대를 Asia/Seoul로 고정 | "내일" 배치 날짜와 저장 시각이 서버 위치와 무관하게 한국 기준이 되도록 |
| 추가 인덱스 | `outfit_items(clothing_id)`, `schedules(outfit_id)` | 의류 · 코디 삭제 시 연쇄 삭제 대상을 빠르게 찾기 위함 (DBML에는 없음) |

## 남은 확인 사항

- 프론트엔드는 ID를 `user_xxx` 같은 문자열로 만들지만, 명세는 bigint 자동 증가다. 2~4단계에서 store를 연결하면서 숫자 ID로 바꿔야 한다.
- 코디 추천이 "실사 사진이 있는 의류만" 후보로 고르는 규칙이 명세에 없다. 6단계 전에 명세에 넣을지 정해야 한다.
- 데모 체험 시 샘플 의류 · 코디 · 배치를 서버와 프론트(localStorage)에 각각 만든다. 3~4단계에서 프론트가 서버 데이터를 쓰게 되면 `src/lib/seedDemo.js`를 제거한다.
- 3~5단계 전까지 의류 · 코디 · 플래너는 여전히 localStorage에 있고, 서버 사용자 ID(숫자)를 ownerId로 쓴다. 서버 도입 전에 만든 로컬 계정 데이터는 더 이상 보이지 않는다.
- 탈퇴 시 업로드한 의류 사진 파일 삭제는 3단계(`/images`)에서 함께 처리한다.
