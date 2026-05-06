# 뭐물꼬(momoolggo) 프로젝트 가이드

> 이 파일은 Claude Code가 매 세션 자동으로 읽는 프로젝트 컨텍스트입니다.
> 모든 작업은 이 가이드의 컨벤션과 규칙을 따릅니다.

---

## 1. 프로젝트 개요

**뭐물꼬**는 음식 배달 플랫폼 팀 프로젝트입니다.

- **현재 상태**: 모놀리식 (Spring Boot, MyBatis 기반)
- **목표**: MSA 전환 + 단순 CRUD JPA 마이그레이션 + 고도화 기능 추가
- **차별화 포인트**: 펫 성장/게임화 시스템 + AI 챗봇 + 룰렛 게임

---

## 2. 기술 스택

| 항목 | 버전/기술 |
|---|---|
| **Spring Boot** | 4.0.3 |
| **Spring Cloud** | 2025.1.x |
| **Java** | 21 (LTS) |
| **빌드 도구** | Gradle (멀티모듈) |
| **영속성** | MyBatis 4.0.1 (복잡 쿼리) + JPA (단순 CRUD) |
| **인증** | Spring Security + JWT (jjwt 0.13.0) <br>※ OAuth2 Client 의존성은 포함되어 있으나 **현재 미사용** (소셜 로그인 안 씀) <br>※ 액세스 15일 / 리프레시 15일 — **개발용 의도 설정** (운영 시 액세스 30분/리프레시 14일로 변경 예정) |
| **외부 API** | 네이버 (지도/검색), 기상청 공공데이터 (Phase 5), Google Gemini (Phase 5), 토스페이먼츠 (Phase 5) |
| **DB 서버** | 학원 공유 MySQL `112.222.157.157:5012` (계정: green2) |
| **서비스 통신** | Spring Cloud OpenFeign (동기), Redis Pub/Sub (비동기) |
| **API Gateway** | Spring Cloud Gateway |
| **캐시** | Redis |
| **DB** | MySQL (1대 / 4 schema) |
| **AI** | Google Gemini API (무료 티어) |
| **결제 PG** | 토스페이먼츠 |
| **기타** | thumbnailator (이미지), spring-dotenv (환경변수) |

> ⚠️ Spring Boot 4.0은 **Jakarta EE 11 + Servlet 6.1** 기반.
> `javax.*`가 아닌 **`jakarta.*`** 패키지를 사용합니다.

---

## 3. 아키텍처 (목표 구조)

### 서비스 구성

| 서비스 | 포트 | 역할 |
|---|---|---|
| **mmg-gateway** | 8000 | API Gateway, 라우팅, 1차 인증 |
| **mmg-main-service** | 8080 | 가게/메뉴/주문/결제/리뷰/펫/쿠폰/룰렛/챗봇 |
| **mmg-auth-service** | 8081 | 회원/주소/약관/JWT/OAuth2 |
| **mmg-rider-service** | 8082 | 라이더/배달/실시간 위치 |
| **mmg-admin-service** | 8083~8084 | 관리자/신고/제재/정산/FAQ |

### 모듈 의존성 그래프

```
       ┌─────────────┐
       │ mmg-common  │  ← 모든 서비스가 의존 (DTO, 예외, JWT 등)
       └──────┬──────┘
              │
   ┌──────────┼──────────┬──────────┬──────────┐
   ▼          ▼          ▼          ▼          ▼
 auth       main       rider      admin     gateway
              │          │          │
              └──── Feign ───┴──────┘
                (서비스 간 통신)
```

**핵심 규칙:**
- 서비스 → `mmg-common` 의존 OK
- 서비스 → 다른 서비스 **코드 의존 절대 금지**
- 서비스 간 통신은 **FeignClient (HTTP)로만**

### 데이터베이스

| Schema | 소속 서비스 | 주요 테이블 |
|---|---|---|
| `my_mmg_auth` | auth-service | user, terms, user_agreements, policies, policy_agreements |
| `my_mmg_main` | main-service | stores, menus, orders, order_items, payments, refunds, carts, reviews, pets, coupons, chat_sessions, store_notices, order_status_log, search_history 등 |
| `my_mmg_rider` | rider-service | rider, delivery, rider_location, delivery_logs |
| `my_mmg_admin` | admin-service | admin, reports, user_penalties, settlements, faqs, notices |

> 📌 **DB 스키마명 컨벤션**: 모든 스키마는 `my_` 접두사로 시작.
> (학원/팀 환경 정책)

> 📌 **MSA 경계 외부 참조는 논리 FK만 사용** (물리 FK 제약 금지).
> 예: `my_mmg_main.orders.user_no` → `my_mmg_auth.user.user_no`는 논리 FK,
> 데이터 정합성은 애플리케이션 레벨(Feign 호출)에서 보장.

---

## 4. 패키지 구조

### 루트 구조

```
momoolggo/
├── settings.gradle
├── build.gradle
├── gradle.properties
├── docs/                          ← 참고 문서 (ERD, API 명세 등)
├── CLAUDE.md                      ← 이 파일
│
├── mmg-common/
├── mmg-auth-service/
├── mmg-main-service/
├── mmg-rider-service/
├── mmg-admin-service/
└── mmg-gateway/
```

### 패키지 컨벤션

```
com.green.mmg.{service}.{domain}
```

**예시:**
- `com.green.mmg.common.jwt.JwtTokenProvider`
- `com.green.mmg.auth.user.UserController`
- `com.green.mmg.main.order.OrderService`
- `com.green.mmg.rider.delivery.DeliveryController`

### 레이어 구조 (각 도메인 내부)

```
{domain}/
├── {Domain}Controller.java
├── {Domain}Service.java
├── {Domain}Repository.java        ← JPA용
├── {Domain}Mapper.java             ← MyBatis용 (필요 시)
├── {Domain}Mapper.xml              ← resources/mapper/
├── entity/
│   └── {Domain}.java
├── dto/
│   ├── {Domain}RequestDto.java
│   └── {Domain}ResponseDto.java
└── exception/
    └── {Domain}Exception.java
```

---

## 5. 코딩 컨벤션

### Java

- **네이밍**: 클래스 PascalCase, 메서드/필드 camelCase, 상수 UPPER_SNAKE_CASE
- **DTO 명명**: `{Domain}RequestDto`, `{Domain}ResponseDto` (또는 기존 `Req`, `Res` 패턴 유지)
- **예외**: 도메인별 커스텀 예외 + `GlobalExceptionHandler` (mmg-common)
- **롬복**: `@Getter`, `@RequiredArgsConstructor` 적극 활용. `@Data`는 엔티티에 사용 금지
- **불변성**: 가능하면 `final` 사용, DTO는 record 또는 불변 클래스
- **주석**: 한글 OK, 다만 JavaDoc은 영문 권장

### DB / SQL

- **테이블명**: snake_case 복수형 또는 단수형 일관성 유지 (기존 컨벤션 따름)
- **컬럼명**: snake_case
- **PK**: `{도메인}_no` 또는 `{도메인}_id` (기존 컨벤션 따름)
- **타입**: `BIGINT AUTO_INCREMENT` PK, `DATETIME` 시각, `BOOLEAN` 플래그
- **엔진/문자셋**: `InnoDB`, `utf8mb4`

### API

- **경로**: `/api/{domain}/{action}` (REST 원칙 준수)
- **응답 형식**: 공통 `ResultResponse` 또는 `ApiResponse` 래퍼 사용 (mmg-common)
- **HTTP 메서드**: GET 조회, POST 생성, PUT/PATCH 수정, DELETE 삭제
- **상태 코드**: 200/201/204, 400/401/403/404, 500 표준 사용

### 영속성 선택 기준

| 기준 | 선택 |
|---|---|
| 단일 엔티티 CRUD | **JPA** |
| 단순 조건 조회 | **JPA** + Spring Data 메서드명 쿼리 |
| 동적 WHERE | **JPA + QueryDSL** 또는 MyBatis |
| 복잡한 다중 JOIN | **MyBatis** |
| 통계/집계 쿼리 | **MyBatis** |
| 네이티브 함수 사용 | **MyBatis** |

> 마이그레이션 시: 기존 MyBatis Mapper를 **즉시 삭제하지 않음**.
> 새 JPA 코드 추가 → 검증 → `@Deprecated` 표시 → 충분히 검증된 후 제거.

### 파일/이미지 저장 정책 ⭐

**원칙: 이미지는 main-service가 단독 책임. 다른 서비스는 URL만 다룸.**

이유: MSA 환경에서 각 서비스가 독립 디스크를 가지면 다른 서비스가 저장한 파일을 못 읽음.
이를 막기 위해 main-service에 파일 저장을 통합.

**저장 방식 (MA에서 그대로 계승):**
- 라이브러리: `net.coobird:thumbnailator:0.4.20`
- 압축: `Thumbnails.size(800, 600).outputQuality(0.8)`
- 파일명: UUID + 확장자
- 저장 경로: 로컬 디스크 `/uploads/{category}/` (예: `/uploads/menu/`, `/uploads/review/`)
- DB 저장: 경로 문자열만 (예: `"/uploads/menu/abc.jpg"`)
- 정적 서빙: main-service의 `WebConfig.java`에서 `addResourceHandler("/uploads/**")`

**서비스별 처리 규칙:**

| 이미지 종류 | 업로드 받는 서비스 | 저장 처리 |
|---|---|---|
| 가게 사진 | main-service | main이 직접 저장 |
| 메뉴 사진 | main-service | main이 직접 저장 |
| 리뷰 사진 | main-service | main이 직접 저장 |
| 펫 아이템 이미지 | main-service (또는 정적 리소스) | main이 직접 저장 |
| 라이더 배달 완료 사진 | rider-service | rider가 받아서 **Feign으로 main에 전달** → main이 저장 |
| 신고 첨부 사진 (관리자) | admin-service | admin이 받아서 **Feign으로 main에 전달** → main이 저장 |

**정적 리소스 라우팅 (Gateway):**
```
GET /uploads/** → Gateway → main-service:8080/uploads/**
```
모든 이미지 조회는 Gateway를 통해 main-service로 라우팅.

**main-service에 두어야 할 Internal API:**
```
POST /internal/files/upload?category={category}
   → MultipartFile 받아 저장 → 저장된 URL 반환
```
rider-service, admin-service가 이 API를 Feign으로 호출.

**향후 확장:**
운영 단계에서 트래픽이 늘면 **AWS S3로 전환**. 코드 영향은 main-service의 업로드 로직만 변경하면 됨 (다른 서비스는 변경 불필요 — 단일 책임의 장점).

**금지 사항:**
- ❌ rider-service, admin-service가 직접 디스크에 파일 저장
- ❌ DB에 Base64 인코딩 이미지 저장
- ❌ 서비스마다 다른 경로 컨벤션 사용

---

## 6. 절대 규칙 (반드시 준수)

### 작업 진행 규칙

1. **계획 먼저, 승인 후 실행**
   - 큰 작업은 반드시 작업 계획을 먼저 제시하고 사용자 승인 후 진행
   - 승인 없이 코드 수정 금지

2. **작업 범위 명시**
   - 이번 작업의 범위를 명확히 표기하고, 그 외는 절대 건드리지 않음
   - "이번 작업은 X만. Y, Z는 다음."

3. **추측 금지**
   - 관련 코드 파악 안 되면 질문하고 멈춤
   - 사용자 의도가 모호하면 확인 후 진행

4. **단계별 작업**
   - 한 번에 한 도메인 / 한 매퍼 / 한 모듈만 수정
   - 변경 후 컴파일 확인, 테스트 후 다음 단계로

5. **커밋 단위 강제**
   - 각 단계 완료 시 커밋 메시지 제안하고 멈춤
   - git 히스토리 깨끗하게 유지

### 코드 변경 규칙

6. **삭제 금지 원칙**
   - 마이그레이션 중 기존 코드 즉시 삭제 금지
   - `@Deprecated` 표시 후 검증 완료된 후에만 제거

7. **API 응답 스펙 동결**
   - 기존 API JSON 응답 구조 변경 금지 (프론트 영향)
   - 필드 추가는 OK, 기존 필드 제거/이름 변경 금지

8. **MSA 경계 준수**
   - 다른 서비스 코드 직접 import 금지
   - 다른 DB 스키마 직접 JOIN 금지
   - 외부 데이터 필요 시 FeignClient 사용

9. **테스트 통과 = 완료 조건**
   - "코드 다 작성했음"은 완료가 아님
   - 컴파일 + 단위 테스트 + 통합 시나리오 통과해야 완료

10. **민감 정보 금지**
    - DB 비밀번호, JWT 시크릿, API 키를 코드에 하드코딩 금지
    - 모든 민감값은 `.env` + `spring-dotenv` 사용
    - `.env`는 `.gitignore` 필수, `.env.example`만 git 추적
    - 학원 공유 환경이라 키 노출 시에도 학원 외부에는 게시하지 않음

11. **도메인 분배 결정 우선순위 (ERD = source of truth)**
    - ERD가 진실의 원천. 도메인 위치(어느 schema에 갈지) 결정 시 ERD 우선 확인.
    - 코드/테이블명 분석은 ERD 보조 자료.
    - ERD vs 코드 충돌 시 ERD 따라감. 단, ERD 오타 의심 시 사용자 결정 받고 decisions.md에 기록.
    - 컬럼명/타입도 ERD 기준 (단순 별칭 차이라도). Phase 2/3에서 같이 정리.
    - **이 원칙은 Phase 1-B-3에서 user_address를 잘못 my_mmg_auth로 보낸 일을 계기로 명문화함 (Phase 1-B-3.5에서 정정).**

---

## 6.5 테스트 작성 규칙 (NAJACKS 재발 방지)

### 가짜 테스트 금지
- `assertNotNull`만으로 끝나는 테스트 금지
- 각 테스트는 다음 중 최소 하나를 정확히 검증해야 함:
  - 반환값 (`assertEquals`, `assertThat` 등)
  - 발생한 예외의 타입 + 메시지
  - DB 상태 변화 (저장 여부, 변경된 필드 값)
  - 외부 호출 발생 여부 (Mockito `verify`)

### 최소 커버리지
- 새 Service 메서드: happy path + 정상 경계 + 예외 케이스 (최소 3개)
- 새 Controller 엔드포인트: 200 / 4xx / 5xx 시나리오
- 외부 호출(Feign, Gemini API): 성공 + 실패 + timeout

### 완료 정의 (Definition of Done)
"코드 작성 완료" ≠ "작업 완료". 다음을 모두 충족해야 완료:
1. `./gradlew :{module}:test` 실행하여 통과
2. 테스트가 실제로 의미 있는 검증인지 `@code-reviewer`로 검증
3. `docs/PROGRESS.md`에 검증 결과(테스트 N개 추가, 통과 여부) 기록

---

## 7. 핵심 도메인 요약

### 🐾 펫 시스템 (mmg-main / pet)

- 회원가입 시 펫 1마리 자동 지급, 종족/이름 선택
- **레벨별 해금:**
  - Lv.1~4: 기본 고객센터 챗봇
  - Lv.5~9: 트렌드 기반 추천 (오늘 인기 카테고리)
  - Lv.10+: 개인 맞춤 추천 (1개월 이력 + 계절 + 시간대 + 날씨)
- 주문 → 간식 자동 지급 → 친밀도/EXP 상승 → 레벨업 → 포인트 보상
- AI 챗봇은 펫 캐릭터 + 4가지 톤 모드(장난꾸러기/미식가/공감/진지)
- AI 엔진: **Google Gemini API (무료 티어)**

### 🤖 챗봇 시스템 (mmg-main / chatbot)

- **단일 엔진**: 펫 챗봇 + 고객센터 챗봇이 같은 Gemini API 사용 (무료 티어)
- **테이블**: `chat_sessions`, `chat_messages`
- **분기**: `entry_point = MYPET / CS`로 컨텍스트 주입 다르게
- **에스컬레이션**: 사용자가 "상담원 연결" 클릭 시 `status = ESCALATED` → 관리자 SSE 알림 → 같은 채팅창에서 사람이 이어받음

### 🎰 룰렛 게임 (mmg-main / roulette)

- 1일 1회 무료
- 등급별 확률: 일반 60% / 레어 25% / 에픽 12% / 전설 3%
- 할인율: 5% / 10% / 15% / 20%
- 결과 유효시간 2시간

### 💳 쿠폰 (mmg-main / coupon)

- 3단 구조: `coupon` (마스터) + `coupon_user` (보유) + `coupon_uses` (사용 이력)
- **선착순 쿠폰은 비관적 락 적용** (`SELECT ... FOR UPDATE` / `@Lock(PESSIMISTIC_WRITE)`)
- `remaining_count` 동시성 제어 핵심

### 🌤️ 날씨 기반 추천 (Lv.10+)

- 공공데이터포털 기상청 API 사용
- **Redis 캐싱**: `weather:grid:{nx}:{ny}` 키, TTL 1시간
- 외부 API 호출 제한 회피 + 응답 속도 향상

### 📦 주문 (mmg-main / order)

- **상태 흐름**: 1(대기) → 3(조리중) → 4(배차) → 5(배달중) → 6(완료)
- 취소: 2 (별도 분기)
- 상태 변경 시 `order_status_log`에 자동 INSERT (이력 추적)
- 주문 시점에 분석용 컬럼 미리 계산 저장: `order_day`, `order_hour`, `order_time_slot`, `order_season`, `weather`

### 🚴 라이더 (mmg-rider)

- 배달 수락/픽업/완료 → `orders.order_state` 업데이트 (Feign으로 main 호출)
- 실시간 위치는 Redis 또는 별도 테이블에 저장 (선택)
- SSE로 사용자/사장에게 위치 푸시

### 👮 신고 자동 제재 (mmg-admin)

- `reports` 누적 횟수에 따라 자동 제재
  - 3회: 주의
  - 5회: 경고
  - 7회: 이용제한
- `user_penalties`에 제재 이력 기록 → Auth 서비스에 Feign으로 사용자 상태 변경

---

## 8. 참고 문서

`docs/` 폴더에 다음 자료가 있습니다 (없으면 사용자에게 위치 확인):

| 파일 | 내용 |
|---|---|
| `docs/erd.md` | 전체 ERD (Mermaid) |
| `docs/api-spec.md` 또는 `.xlsx` | API 명세서 (전체 엔드포인트) |
| `docs/requirements.md` | 기능 명세서 |
| `docs/architecture.md` | 시스템 아키텍처 다이어그램 |
| `docs/current-structure.txt` | 현재 패키지 구조 (`tree -L 4` 결과) |

작업 전 관련 문서 먼저 확인 권장. 충돌 발생 시 사용자에게 확인 요청.

---

## 9. 작업 단계 (Phase)

MSA 전환은 다음 순서로 진행합니다:

| Phase | 내용 | 상태 |
|---|---|---|
| **Phase 0** | 멀티모듈 Gradle 스켈레톤 + 빈 서비스 6개 hello world | ⏳ 진행 예정 |
| **Phase 1** | Auth 서비스 분리 (가장 독립적인 도메인부터) | 대기 |
| **Phase 2** | 도메인별 코드 이동 (Main / Rider / Admin) | 대기 |
| **Phase 3** | MyBatis → JPA 선별 마이그레이션 | 대기 |
| **Phase 4** | FeignClient 도입, Gateway 라우팅, 서비스 간 통신 | 대기 |
| **Phase 5** | 신규 기능 구현 (펫, 룰렛, 챗봇, 신고 등) | 대기 |
| **Phase 6** | 고도화 (Redis Pub/Sub, Outbox 패턴, 성능 최적화) | 대기 |

각 Phase 시작 전 현재 위치 확인. 사용자가 Phase 명시하지 않으면 질문.

---

## 10. 자주 쓰는 명령어

### Gradle (루트에서)

```bash
./gradlew build                              # 전체 빌드
./gradlew :mmg-auth-service:bootRun          # auth만 실행
./gradlew :mmg-main-service:bootRun          # main만 실행
./gradlew :mmg-common:build                  # common 라이브러리 빌드
./gradlew clean build -x test                # 테스트 제외 빌드
```

### 멀티 서비스 동시 실행 (개발)

```bash
# 별도 터미널 5개:
./gradlew :mmg-gateway:bootRun
./gradlew :mmg-auth-service:bootRun
./gradlew :mmg-main-service:bootRun
./gradlew :mmg-rider-service:bootRun
./gradlew :mmg-admin-service:bootRun
```

### Git

```bash
# 작업 단위 커밋 컨벤션
git commit -m "feat(auth): 회원가입 API 구현"
git commit -m "refactor(main): OrderMapper JPA 마이그레이션"
git commit -m "fix(rider): 배달 상태 동기화 누락 처리"
```

---

## 11. 사용자(준하님) 작업 스타일 메모

- 한국어로 소통
- 백엔드 중심 (JPA, Spring Boot, MSA), Vue 3 프론트도 다룸
- 학원 팀 프로젝트 진행 중 (팀 리더)
- 단계별 명확한 가이드 선호
- 코드 자동 변경보다 **계획 검토 후 진행** 선호
- 모르는 개념(예: 멀티모듈, Redis, Outbox)은 짚어서 설명 후 진행

---

## 12. 비상 시 행동 지침

### 모르는 코드를 만났을 때
- 추측해서 작성하지 말고 **사용자에게 질문**
- "이 코드가 어디에 쓰이나요?"
- "기존 동작이 어떤가요?"

### 충돌하는 요구사항이 있을 때
- 두 요구사항을 모두 사용자에게 제시
- 사용자가 결정할 수 있도록 트레이드오프 명확히 설명

### 빌드/테스트가 깨졌을 때
- 즉시 작업 중단
- 마지막 변경사항 보고
- 롤백 vs 수정 둘 중 하나 선택할 수 있게 안내

### 의존성 충돌 발생 시
- Spring Boot 4.0.x ↔ Spring Cloud 2025.1.x 호환성 우선 확인
- BOM(`spring-cloud-dependencies`)으로 버전 관리
- 임의 버전 다운그레이드 금지 → 사용자 확인

---

## 끝

이 가이드는 작업 진행에 따라 업데이트됩니다.
변경이 필요하면 사용자에게 먼저 제안하고 승인받은 후 수정합니다.
