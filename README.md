# 뭐물꼬 (MSA 버전)

음식 배달 플랫폼 — 모놀리식에서 MSA로 전환 중인 팀 프로젝트

> 📌 이전 모놀리식(MA) 버전은 `C:\CODDING\PROJECT\MOMOOLGGO\`에 보존되어 있습니다.

---

## 🛠 기술 스택

| 분류 | 기술 |
|---|---|
| Language | Java 21 (LTS) |
| Framework | Spring Boot 4.0.3, Spring Cloud 2025.1.x |
| Build | Gradle (멀티모듈) |
| 영속성 | MyBatis 4.0.1 + JPA (선별 도입) |
| 인증 | Spring Security + JWT (jjwt 0.13.0) |
| 서비스 통신 | Spring Cloud OpenFeign, Redis Pub/Sub |
| Gateway | Spring Cloud Gateway |
| 캐시 | Redis |
| DB | MySQL (4 schema 분리) |
| AI | Anthropic Claude API |
| 외부 API | 네이버 (지도/검색), 기상청 |

---

## 🏗 서비스 구성

| 서비스 | 포트 | 역할 |
|---|---|---|
| `mmg-gateway` | 8000 | API Gateway, 라우팅, 1차 인증 |
| `mmg-main-service` | 8080 | 가게/메뉴/주문/결제/리뷰/펫/쿠폰/룰렛/챗봇 |
| `mmg-auth-service` | 8081 | 회원/주소/약관/JWT |
| `mmg-rider-service` | 8082 | 라이더/배달/실시간 위치 |
| `mmg-admin-service` | 8083 | 관리자/신고/제재/정산/FAQ |

```
       ┌─────────────┐
       │ mmg-common  │  공통 라이브러리 (DTO, JWT, 예외)
       └──────┬──────┘
              │
   ┌──────────┼──────────┬──────────┬──────────┐
   ▼          ▼          ▼          ▼          ▼
 auth       main       rider      admin     gateway
              │          │          │
              └──── Feign ───┴──────┘
```

---

## 💾 데이터베이스

| Schema | 서비스 |
|---|---|
| `my_mmg_auth` | auth-service |
| `my_mmg_main` | main-service |
| `my_mmg_rider` | rider-service |
| `my_mmg_admin` | admin-service |

DB 서버: 학원 공유 MySQL (`112.222.157.157:5012`)

---

## 🚀 실행 방법

### 사전 준비

1. Java 21 설치 확인: `java --version`
2. `.env` 파일 생성 (`.env.example` 참고)
3. MySQL 4개 스키마 생성 (`docs/ddl/` 참고)
4. Docker Desktop 설치 (Redis 컨테이너 실행용)

### Redis 실행 (Phase 4-C부터 — auth RT 저장)

```bash
# 백그라운드로 Redis 띄우기
docker compose up -d redis

# 상태 확인
docker compose ps

# 종료
docker compose down
```

기본 포트 6379. 포트 변경은 `.env`의 `REDIS_PORT` 수정.

### 환경별 설정 분리 원칙

- 로컬 (`application.yml` + `.env`): 폴백값/유연한 값 (ddl-auto=update 허용, REDIS_HOST=localhost 등)
- prod (`application-prod.yml` + `mmg-common/application-*-prod.yml`): K8s 고정값 (ddl-auto=none, redis-master.infra.svc.cluster.local 등)

redis/kafka 주소를 변경할 때:
- 로컬: `.env` 수정
- prod: `mmg-common/src/main/resources/application-{redis|kafka}-prod.yml` 수정 → 4개 서비스 모두 자동 반영

### 빌드 & 실행

```bash
# 전체 빌드
./gradlew build

# 각 서비스 실행 (별도 터미널)
./gradlew :mmg-gateway:bootRun
./gradlew :mmg-auth-service:bootRun
./gradlew :mmg-main-service:bootRun
./gradlew :mmg-rider-service:bootRun
./gradlew :mmg-admin-service:bootRun
```

---

## 📚 문서

자세한 내용은 `docs/` 폴더 참고:

- [`CLAUDE.md`](./CLAUDE.md) — 프로젝트 전체 가이드 (개발 컨벤션, 절대 규칙)
- [`docs/migration-plan.md`](./docs/migration-plan.md) — Phase별 진행 체크리스트
- [`docs/decisions.md`](./docs/decisions.md) — 의사결정 기록
- [`docs/erd/`](./docs/erd/) — ERD 다이어그램
- [`docs/api-spec/`](./docs/api-spec/) — API 명세서
- [`docs/architecture/`](./docs/architecture/) — 시스템 아키텍처

---

## 🗂 프로젝트 구조

```
MOMOOLGGO_MSA/
├── CLAUDE.md                    ← Claude Code 가이드
├── README.md                    ← 본 파일
├── .gitignore
├── .env                         ← 환경변수 (Git 제외)
├── .env.example                 ← 환경변수 템플릿
│
├── settings.gradle              ← Phase 0에서 생성
├── build.gradle                 ← Phase 0에서 생성
│
├── docs/                        ← 설계 문서
│
├── mmg-common/                  ← Phase 0에서 생성
├── mmg-auth-service/            ← Phase 0에서 생성
├── mmg-main-service/            ← Phase 0에서 생성
├── mmg-rider-service/           ← Phase 0에서 생성
├── mmg-admin-service/           ← Phase 0에서 생성
└── mmg-gateway/                 ← Phase 0에서 생성
```

---

## 👥 팀

대구 그린아트아카데미 502기 MSA 클래스 팀 프로젝트

---

## 📝 라이선스

학습용 / 비공개 프로젝트
