# today-outfit backend

Spring Boot 4 · Java 21 · PostgreSQL 17 · Flyway

## 실행

```sh
# 0. 비밀값 파일 준비 (최초 1회) — DB_PASSWORD, JWT_SECRET을 채운다
cp .env.example .env

# 1. DB 실행 (Docker Desktop 필요)
docker compose up -d

# 2. 서버 실행 → http://localhost:8080/api
./gradlew bootRun
```

헬스 체크: `GET http://localhost:8080/api/actuator/health` → `{"status":"UP"}`

프론트엔드(`npm run dev`)는 `/api` 요청을 Vite 프록시로 이 서버에 넘긴다.

## 설정

키 · 비밀번호는 `backend/.env`에 두고 커밋하지 않는다. 항목과 생성 방법은 [.env.example](.env.example)에 있다.

- `docker compose`는 같은 폴더의 `.env`를 자동으로 읽는다.
- Spring Boot는 `spring.config.import`로 `.env`를 읽는다. 작업 디렉터리가 `backend/`일 때(`./gradlew bootRun`, `./gradlew test`) 적용된다.
- 배포 환경에서는 `.env` 파일 없이 같은 이름의 환경변수로 주입한다. 환경변수가 `.env`보다 우선한다.

| 변수 | 필수 | 설명 |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | | 기본값 `localhost` / `5432` / `today_outfit` |
| `DB_USERNAME` / `DB_PASSWORD` | ✅ | DB 계정 |
| `DB_URL` | | 전체 JDBC URL. 지정하면 HOST/PORT/NAME 대신 사용 |
| `JWT_SECRET` | ✅ (2단계부터) | JWT 서명 키, 32바이트 이상 Base64 |
| `PORT` | | 서버 포트, 기본 `8080` |

`DB_PASSWORD`를 바꿀 때: 이미 만들어진 DB 볼륨에는 새 값이 적용되지 않으므로 아래처럼 DB 계정 비밀번호도 함께 바꾼다.

```sh
docker exec -it today-outfit-db psql -U today_outfit -c "ALTER USER today_outfit WITH PASSWORD '<새 비밀번호>'"
```

스키마는 `src/main/resources/db/migration`의 Flyway 마이그레이션으로 관리한다.
