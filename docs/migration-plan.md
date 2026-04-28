# MSA 전환 진행 체크리스트

> 각 단계 완료 시 `[ ]` → `[x]`로 변경
> Phase 진행 중 발견된 이슈는 하단 "이슈 로그"에 기록

---

## 📍 현재 위치

**Phase 0-B — 5개 서비스 + Gateway** (대기 중, 0-A 완료)

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
- [ ] `mmg-auth-service` 생성 + AuthApplication.java + application.yml
- [ ] `mmg-main-service` 생성 + MainApplication.java + application.yml
- [ ] `mmg-rider-service` 생성 + RiderApplication.java + application.yml
- [ ] `mmg-admin-service` 생성 + AdminApplication.java + application.yml
- [ ] `mmg-gateway` 생성 + GatewayApplication.java + application.yml
- [ ] 각 서비스에 임시 HelloController (`GET /hello` → "Hello from {service}")
- [ ] 각 서비스 단독 기동 성공
- [ ] 5개 서비스 동시 기동 성공
- [ ] Gateway 통해 각 서비스 라우팅 확인
- [ ] **Phase 0 완료 커밋**

---

## Phase 1 — Auth 서비스 분리

### 1-A. mmg-common에 공통 코드 이동
- [ ] `JwtTokenProvider`, `TokenAuthenticationFilter` → mmg-common/jwt
- [ ] `JwtUser`, `UserPrincipal` → mmg-common/model
- [ ] `ResultResponse` → mmg-common/dto
- [ ] `ConstJwt` → mmg-common/constants
- [ ] `MyCookieUtil` → mmg-common/util
- [ ] `WebSecurityConfiguration` 베이스 → mmg-common (오버라이드 가능하게)
- [ ] mmg-common 빌드 성공

### 1-B. Auth 도메인 이동
- [ ] `application/user/*` → mmg-auth-service (Review 관련 제외)
- [ ] `application/address/*` → mmg-auth-service
- [ ] my_mmg_auth DB 생성 + 테이블 이전
- [ ] 패키지명 변경 (`com.green.momoolggo` → `com.green.mmg.auth`)
- [ ] auth-service 단독 기동 성공
- [ ] 기존 `/api/user/**`, `/api/auth/**` API 응답 100% 동일 검증
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
