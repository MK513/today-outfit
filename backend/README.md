# today-outfit backend

Spring Boot 4 · Java 21 · PostgreSQL 17 · Flyway

## 실행

```sh
# 1. DB 실행 (Docker Desktop 필요)
docker compose up -d

# 2. 서버 실행 → http://localhost:8080/api
./gradlew bootRun
```

헬스 체크: `GET http://localhost:8080/api/actuator/health` → `{"status":"UP"}`

프론트엔드(`npm run dev`)는 `/api` 요청을 Vite 프록시로 이 서버에 넘긴다.

## 설정

환경변수로 덮어쓸 수 있다.

| 변수 | 기본값 |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/today_outfit` |
| `DB_USERNAME` / `DB_PASSWORD` | `today_outfit` |
| `DB_PORT` (compose) | `5432` |
| `PORT` | `8080` |

스키마는 `src/main/resources/db/migration`의 Flyway 마이그레이션으로 관리한다.
