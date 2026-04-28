# MSA 전환 진행 체크리스트

> 각 단계 완료 시 `[ ]` → `[x]`로 변경
> Phase 진행 중 발견된 이슈는 하단 "이슈 로그"에 기록

---

## 📍 현재 위치

**Phase 1-B-2 — WebSecurityConfiguration 리팩 + CORS 환경변수화** (대기 중, 1-B-1 완료)

---

## Phase 0 — 멀티모듈 스켈레톤

### 0-A. 루트 + 공통 모듈
- [x] `settings.gradle` 작성 (모듈 6개 선언)
- [x] 루트 `build.gradle` 작성 (subprojects 공통 설정)
- [x] `gradle.properties` 작성
- [x] `mmg-common` 모듈 생성
- [x] `mmg-common/build.gradle` 작성 (라이브러리, bootJar 비활성)
- [x] `mmg-common`만 단독 빌드 성공 확인

### 0-B. 5개 서비스 + Gateway
- [x] `mmg-auth-service` 생성 + AuthApplication.java + application.yml
- [x] `mmg-main-service` 생성 + MainApplication.java + application.yml
- [x] `mmg-rider-service` 생성 + RiderApplication.java + application.yml
- [x] `mmg-admin-service` 생성 + AdminApplication.java + application.yml
- [x] `mmg-gateway` 생성 + GatewayApplication.java + application.yml
- [x] 각 서비스에 임시 HelloController (`GET /api/{prefix}/hello`)
- [x] 각 서비스 단독 기동 성공 (4개 서비스 + gateway 순차 검증)
- [ ] 5개 서비스 동시 기동 성공 (메모리 부담으로 미수행 — 사용자 수동 검증 권장)
- [x] Gateway 통해 라우팅 확인 (auth 경로 검증, 다른 3개도 동일 패턴)
- [x] **Phase 0 완료 커밋**

---

## Phase 1 — Auth 서비스 분리

### 1-A. mmg-common에 공통 코드 이동
- [x] `JwtTokenProvider`, `JwtTokenManager`, `TokenAuthenticationFilter` → mmg-common/jwt
- [x] `JwtUser`, `UserPrincipal` → mmg-common/model
- [x] `ResultResponse` → mmg-common/dto
- [x] `ConstJwt` → mmg-common/constants
- [x] `MyCookieUtil` → mmg-common/util
- [x] `WebSecurityConfiguration` 베이스 → mmg-common (그대로 이동, base/override 리팩은 1-B에서)
- [x] mmg-common 빌드 성공
- [x] 전체 빌드 성공 (6개 모듈)
- ❌ **WebConfig는 의도적으로 미포함** — main-service 전용 (이미지 정적 리소스 핸들러), Phase 2에서 직접 이동

### 1-A 후속 TODO (1-B 완료 시 일괄 처리)
- [ ] `JwtTokenManager.java` line 71 주석 잔재 정리 (`com.green.greengram` 참조)
- [ ] `WebSecurityConfiguration` base/override 패턴 리팩토링 (도메인별 경로 매칭이 mmg-common에 박혀있는 상태 해소)
- [ ] `CorsConfigurationSource`의 `localhost:5173` 하드코딩 → 환경변수화 검토
- [ ] mmg-common 빈을 각 서비스에서 활성화하는 방법 결정 (`scanBasePackages` vs `@Import` vs auto-configuration)

### 1-B. Auth 도메인 이동 (5단계로 세분)

#### 1-B-1. my_mmg_auth DB 생성 + 데이터 마이그레이션
- [x] `my_mmg_auth` schema 생성 (utf8mb4_unicode_ci)
- [x] user, address DDL 적용 (collation 변경 외 원본 동일)
- [x] user 15행 + address 20행 INSERT (schema 간 INSERT...SELECT)
- [x] 검증 5종 통과 (row count, AUTO_INCREMENT, MAX PK, FK 정합성, orphan 0)
- [x] 백업 dump (`docs/ddl/dump-*.sql`, .gitignore 차단)
- [x] `docs/ddl/README.md` 작성 (Phase 2 재사용용)

#### 1-B-2. WebSecurityConfiguration base/override 리팩토링
- [ ] CORS origin 환경변수화 (`CORS_ALLOWED_ORIGINS`, 콤마 구분)
- [ ] base 클래스에 공통 시큐리티 설정만 남기기
- [ ] auth-service에 자체 SecurityConfig 추가 (도메인별 경로 매칭)
- [ ] mmg-common 빈 활성화 검증 (`@SpringBootApplication(scanBasePackages = {...})`)

#### 1-B-3. user 도메인 코드 → auth-service
- [ ] `application/user/*` 이동 (Review 관련 제외)
- [ ] 패키지명 변경 (`com.green.momoolggo` → `com.green.mmg.auth`)
- [ ] MyBatis mapper xml 이동 + 경로 조정
- [ ] auth-service 단독 기동 + `/api/user/**` 호출 검증

#### 1-B-4. address 도메인 코드 → auth-service
- [ ] `application/address/*` 이동
- [ ] 패키지/import 변경
- [ ] `/api/address/**` 호출 검증

#### 1-B-5. 통합 검증
- [ ] 회원가입/로그인 시나리오 (기존 API와 응답 100% 동일)
- [ ] auth-service 안정 기동 확인
- [ ] **Phase 1 완료 커밋**

---

## Phase 2 — Main 서비스 도메인 이동

### 2-A. 기존 도메인 이동
- [ ] `application/store/*` → mmg-main-service
- [ ] `application/owner/*` → mmg-main-service
- [ ] `application/cart/*` → mmg-main-service
- [ ] `application/order/*` → mmg-main-service
- [ ] `application/payment/*` → mmg-main-service
- [ ] Review 관련 (user/model/Review*.java) → mmg-main-service/review

### 2-B. DB 분리
- [ ] my_mmg_main DB 생성 + 테이블 이전
- [ ] auth ↔ main 사이 데이터 정합성 확인 (논리 FK)

### 2-C. Rider/Admin 빈 껍데기
- [ ] my_mmg_rider DB 생성
- [ ] my_mmg_admin DB 생성
- [ ] **Phase 2 완료 커밋**

---

## Phase 3 — MyBatis → JPA 선별 마이그레이션

### 3-A. 마이그레이션 대상 분석
- [ ] 각 Mapper의 모든 쿼리 분류 (JPA 가능 / QueryDSL / MyBatis 유지)
- [ ] 마이그레이션 우선순위 표 작성

### 3-B. 단순 CRUD 우선 마이그레이션
- [ ] auth-service: User Repository (JPA)
- [ ] main-service: Store Repository (JPA)
- [ ] main-service: Cart Repository (JPA)
- [ ] (그 외 단순 도메인들)

### 3-C. 복잡 쿼리는 MyBatis 유지
- [ ] OrderMapper (다중 조인, 통계) → MyBatis 유지
- [ ] StoreMapper (검색, 필터) → 일부 QueryDSL 검토
- [ ] **Phase 3 완료 커밋**

---

## Phase 4 — 서비스 간 통신 인프라

### 4-A. FeignClient 도입
- [ ] mmg-common/feign 공통 설정
- [ ] main → auth: 사용자 정보 조회 FeignClient
- [ ] rider → main: 주문 상태 변경 FeignClient
- [ ] admin → auth: 사용자 제재 FeignClient

### 4-B. Gateway 라우팅 완성
- [ ] 모든 API 경로 매핑
- [ ] JWT 1차 검증을 Gateway에서 처리
- [ ] `/uploads/**` → main-service 라우팅

### 4-C. Redis 도입
- [ ] Redis 서버 구축
- [ ] 토큰 저장 (auth-service)
- [ ] 날씨 캐시 (main-service)
- [ ] Pub/Sub 채널 설계
- [ ] **Phase 4 완료 커밋**

---

## Phase 5 — 신규 기능 구현

### 5-A. 펫 시스템 (main-service)
- [ ] 엔티티/Repository 작성 (pet, pet_items, pet_inventory, pet_feedings)
- [ ] 펫 생성 API (회원가입 시 자동 지급)
- [ ] EXP/친밀도/레벨업 로직
- [ ] 간식 주기 (주문 → 자동 지급)

### 5-B. 챗봇 (main-service)
- [ ] chat_sessions, chat_messages 테이블
- [ ] Gemini API 연동 (mmg-common/gemini)
- [ ] 펫 챗봇 (entry_point=MYPET)
- [ ] CS 챗봇 (entry_point=CS)
- [ ] 4가지 톤 모드 분기
- [ ] 상담원 에스컬레이션 (status=ESCALATED)
- [ ] 무료 티어 호출 제한 대응 (분당 15회 / 일 1,500회)

### 5-C. 결제 — 토스페이먼츠 (main-service)
- [ ] 토스페이먼츠 테스트 키 발급
- [ ] 결제 위젯 연동 (Vue 프론트)
- [ ] 결제 승인 API (백엔드)
- [ ] 결제 취소/환불 API
- [ ] 결제 실패 처리
- [ ] 영수증 발급

### 5-D. 룰렛 게임 (main-service)
- [ ] roulette_results 테이블
- [ ] 등급별 확률 로직 (60/25/12/3)
- [ ] 1일 1회 무료 정책
- [ ] 결과 유효시간 2시간

### 5-E. 쿠폰 동시성 (main-service)
- [ ] 비관적 락 (`@Lock(PESSIMISTIC_WRITE)`)
- [ ] 선착순 쿠폰 발급 테스트

### 5-F. 라이더 서비스 구현
- [ ] 라이더 정보, 배달 처리 로직
- [ ] 실시간 위치 (Redis)
- [ ] SSE 알림

### 5-G. 관리자 서비스 구현
- [ ] 신고 처리 (자동 제재 3/5/7회)
- [ ] 정산 계산 (가게별 월별)
- [ ] FAQ, 시스템 공지

### 5-H. 추가 테이블 구현 (3개)
- [ ] `order_status_log` 트리거/서비스 레이어
- [ ] `search_history` 검색 시 INSERT
- [ ] `store_notices` CRUD
- [ ] **Phase 5 완료 커밋**

---

## Phase 6 — 고도화 (선택)

- [ ] Outbox 패턴 (이벤트 발행 안정성)
- [ ] Kafka 도입 (비동기 메시징)
- [ ] 분산 트랜잭션 (Saga 패턴)
- [ ] 모니터링 (Actuator + Prometheus + Grafana)
- [ ] 로그 중앙화 (ELK 또는 Loki)
- [ ] CI/CD 파이프라인
- [ ] Docker Compose / Kubernetes

---

## 📝 이슈 로그

### YYYY-MM-DD
- (이슈 발생 시 여기에 기록)

---

## 📊 통계

| Phase | 시작일 | 완료일 | 소요일 |
|---|---|---|---|
| Phase 0 | - | - | - |
| Phase 1 | - | - | - |
| Phase 2 | - | - | - |
| Phase 3 | - | - | - |
| Phase 4 | - | - | - |
| Phase 5 | - | - | - |
| Phase 6 | - | - | - |
