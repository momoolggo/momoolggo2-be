# 설계 의사결정 기록

> 프로젝트 진행 중 내린 모든 중요한 결정과 그 이유를 기록.
> 새 결정이 추가될 때마다 아래에 누적.

---

## 2026-04-28 (Phase 0 시작 직전 결정사항)

### 프로젝트 구조

| 결정 | 선택 | 이유 |
|---|---|---|
| **새 프로젝트 폴더** | `MOMOOLGGO_MSA` 별도 생성 | 기존 MA 코드 보존, 안전한 작업 |
| **기존 MA 처리** | 그대로 보존 | 비교/참조용, Phase 1~2에서 자주 봐야 함 |
| **모듈 구성** | common + auth + main + rider + admin + gateway (6개) | 표준 Spring Cloud MSA 패턴 |
| **패키지명** | `com.green.mmg.{service}.{domain}` | 짧고 모듈명과 매칭, 기존 `com.green.momoolggo`에서 변경 |

### 기술 스택

| 결정 | 선택 | 이유 |
|---|---|---|
| **Spring Boot** | 4.0.3 | 기존 MA 버전 유지 |
| **Spring Cloud** | 2025.1.x | Spring Boot 4.0과 호환 |
| **Java** | 21 (LTS) | 기존 MA 버전 유지 |
| **영속성** | MyBatis 유지 + 단순 CRUD JPA 도입 | 기존 자산 활용 + 점진적 개선 |
| **OAuth2** | 미사용 | 의존성은 존속, 소셜 로그인 안 씀 |

### 데이터베이스

| 결정 | 선택 | 이유 |
|---|---|---|
| **DB 분리 방식** | 4개 schema (`my_mmg_auth`, `my_mmg_main`, `my_mmg_rider`, `my_mmg_admin`) | MSA 기본 원칙 |
| **DB 서버** | 학원 공유 서버 `112.222.157.157:5012` | 팀원 모두 접속 가능 |
| **스키마명 접두사** | `my_` 필수 | 학원 MySQL 권한 정책 (`my_%` 만 무제한 생성) |
| **MSA 경계 외부 참조** | 논리 FK만 (물리 FK 제약 X) | 다른 schema 간 물리 제약 불가, Feign으로 정합성 |

### JWT / 보안

| 결정 | 선택 | 이유 |
|---|---|---|
| **액세스 토큰 만료** | 15일 | **개발용 의도** (운영 시 30분으로 변경 예정) |
| **리프레시 토큰 만료** | 15일 | 개발용 |
| **JWT 시크릿** | mmg-common이 보유, 모든 서비스가 공유 | 어느 서비스에서 발급해도 다른 서비스가 검증 가능 |

### 이미지 저장

| 결정 | 선택 | 이유 |
|---|---|---|
| **저장 책임** | main-service 단독 (C 옵션) | MSA에서 디스크 분산되면 다른 서비스가 못 읽음 |
| **저장 방식** | 로컬 디스크 + Thumbnailator 압축 | 기존 MA 방식 그대로 계승 |
| **다른 서비스가 이미지 받을 때** | rider/admin이 받아서 Feign으로 main에 전달 | 단일 책임 유지 |
| **운영 시 전환 계획** | AWS S3 (Phase 6 또는 운영 직전) | 코드 영향 main만 받음 |

### ERD 추가 테이블 (3개)

| 테이블 | 위치 | 결정 이유 |
|---|---|---|
| `order_status_log` | my_mmg_main | MSA에서 여러 서비스가 주문 상태 변경 → 이력 추적 필수, 분쟁 처리, 평균 처리시간 통계 |
| `search_history` | my_mmg_main | 사용자별 최근 검색어, 인기 검색어 통계 |
| `store_notices` | my_mmg_main | 가게 사장 자율 게시 (휴무, 한정 메뉴), 마케팅 이벤트 기능 일부 흡수 |

### ERD 추가 검토 후 제외한 테이블

| 테이블 | 제외 이유 |
|---|---|
| `notifications` | 알림 페이지 미기획, SSE만으로 충분 |
| `user_devices` | 웹 기반 프로젝트라 FCM 토큰 관리 불필요 |
| `events` | 마케팅 이벤트 미사용, store_notices가 일부 대체 |
| `outbox` | Phase 6 고도화 단계로 미룸 |
| `refunds` 분리 | 이미 존재 |
| 신고/정산/약관 | 이미 존재 (reports/settlements/terms) |

### 게임 시스템

| 결정 | 선택 | 이유 |
|---|---|---|
| **룰렛 무료 횟수** | 1일 1회 무료만 | 100P 소모 옵션 제거 (단순화) |
| **펫 레벨 해금** | Lv.1~4 기본 / Lv.5~9 트렌드 / Lv.10+ 개인화 | 단일 Claude API + 시스템 프롬프트 데이터 차등 |
| **쿠폰 동시성** | 비관적 락 (DB 기반) | 선착순 안전성 우선, Redis는 보조 |

### 챗봇 통합

| 결정 | 선택 | 이유 |
|---|---|---|
| **펫 챗봇 ↔ CS 챗봇** | `chat_sessions.entry_point`로 분기 (MYPET / CS) | 단일 엔진, 데이터/톤만 다르게 |
| **상담원 연결** | `chat_sessions.status=ESCALATED` → 관리자 SSE | 별도 1:1 문의함 불필요 |

### 외부 API 선택

| 결정 | 선택 | 이유 |
|---|---|---|
| **AI 모델** | Google Gemini API (무료 티어) | 학원/팀프 환경, 비용 0원, 분당 15회/일 1,500회 무료 |
| **결제 PG** | 토스페이먼츠 | 테스트 키 즉시 발급, 한국 시장 표준, 문서/SDK 우수 |
| **결제 환경** | 테스트 키 사용 (실결제 X) | 학습 목적, 운영 가면 라이브 키로 전환 |
| **지도/주소** | 네이버 (이미 보유) | 학원 키 재활용 |
| **날씨** | 기상청 공공데이터 | 무료, 한국 격자 좌표계 표준 |

---

## 향후 결정 필요 항목

- [ ] Phase 4: Service Discovery 도입 여부 (Eureka vs 직접 라우팅)
- [ ] Phase 4: 분산 트랜잭션 처리 방식 (Saga vs 보상 트랜잭션 vs Outbox)
- [ ] Phase 5: Gemini API 호출 시 캐싱 전략 (동일 질문 반복 처리)
- [ ] Phase 6: 모니터링 스택 선택 (Prometheus+Grafana vs Datadog vs 기타)
- [ ] 운영 직전: 이미지 저장 S3 전환 시점

---

## 결정 변경 이력

> 이전 결정을 뒤집을 때만 기록 (왜 바뀌었는지)

### 2026-04-28 (Phase 1-B-3.5 — UserAddress 위치 정정)

**기존 (Phase 1-B-3)**: `user_address` (또는 `address`) 테이블 → my_mmg_auth schema
**변경 후**: `address` 테이블 → my_mmg_main schema

| 항목 | 결정 |
|---|---|
| **address 테이블 위치** | my_mmg_auth → **my_mmg_main** (ERD 따름, 초록 그룹 확인됨) |
| **결정 근거** | ERD = source of truth (CLAUDE.md §6.11 신설). Phase 1-B-3 정찰에서 코드 분석만으로 잘못 판단함 |
| **컬럼명 변경** | ERD 따라: `lat` → `latitude`, `lng` → `longitude`, `address_detail` VARCHAR(300) → VARCHAR(200) |
| **`user_no2` 컬럼명** | ERD에 `user_no2`로 표기됐으나 다른 모든 테이블이 `user_no` 사용 → **ERD 오타로 판단**, `user_no` 유지 |
| **URL 변경** | `/api/user/address/**` → `/api/address/**` (옵션 B 채택, 단일 책임 명확) |
| **회원가입 흐름** | 옵션 D-1 (BFF 패턴) — 프론트가 두 API 호출 (`/api/user/join` + `/api/address`) |
| **POST /api/user/join 응답 변경** | `null` → `{userNo, name, role, atExpiresAt, storeName}` + AT/RT HttpOnly 쿠키 발급 |
| **CLAUDE.md §6.7 (API 응답 동결) 예외 처리** | join 응답 변경은 명시적 예외. 사유: MSA 분리 후 단일 트랜잭션 깨짐 → 프론트 BFF 패턴 필요. 호환성: 기존 필드 추가만 (resultData null → 객체), 프론트는 안전하게 새 필드 활용 가능 |
| **원자성** | 약함 (1번 성공 + 2번 실패 가능). 임시 처리: 사용자에게 안내 메시지. 강화는 Phase 6 Saga 패턴 검토 |
| **외부 FK 정합성 (사용자 탈퇴 시 cleanup)** | Phase 4-A에서 Saga/Outbox로 결정 |

### 2026-04-28 (Phase 2-E — review/owner_comment 정리)

| 항목 | 결정 |
|---|---|
| **owner_comment 테이블** | ERD에 그려져 있으나 원본/신규 DB 양쪽 모두 미존재. ERD 작성자의 미래 의도 추정 — `review_reply`와 의미 동일 (사장 답글). |
| **review_reply 유지** | 이미 `my_mmg_main.review_reply`로 마이그레이션됨 + 데이터 0행 + 코드 미사용 (Phase 5에서 사장 답글 기능 신규 시 사용 예정) |
| **owner_comment 처리** | 테이블 자체가 없으므로 DROP 작업 불필요. ERD에서 owner_comment 박스 제거 권장 (학원 ERD 작성자에게 전달 필요). 또는 review_reply의 컬럼/이름을 ERD에 맞춰 정리 (Phase 5 결정) |
| **review 도메인** | Phase 1에서 user/에 섞여있던 review 5 endpoint + 9 SQL을 main-service.review로 분리 신규 작성. 경로 `/api/user/review/**` 유지 (api-spec 동결) |

### 2026-04-28 (Phase 4-A — Feign 도입 + cross-schema JOIN 해소)

| 항목 | 결정 |
|---|---|
| **Phase 진입 순서 변경** | 기존 Phase 3 → 4 → 5 / 신규 옵션 3: **4-A → 3 → 4-B/C → 5**. 사유: cross-schema JOIN 5개가 깨진 채 Phase 3 가면 회귀 검증 사각지대 |
| **UserBriefDto 위치** | `mmg-common/dto/feign/` — 5개 서비스 공통 사용 |
| **AuthFeignClient 위치** | `mmg-common/feign/` (interface) — `@EnableFeignClients(basePackages="com.green.mmg.common.feign")` |
| **Internal API 응답에 address** | 빈 문자열 — main이 자체 user_address 조회로 채움 (Phase 1-B-3.5 ERD 준수) |
| **Batch endpoint** | `GET /internal/auth/users?ids=1,2,3` 채택 — N+1 회피, 1회 100개 권장 |
| **`/internal/**` 보안** | Phase 4-A: AuthSecurityConfig permitAll. Phase 4-B Gateway가 외부 차단. Phase 6 mTLS/JWT 검토 |
| **외부 FK 정합성 (Saga/Outbox)** | Phase 4-D 또는 Phase 6으로 분리 — Phase 4-A는 read-side만, write-side 별개 |
| **OwnerInfoDto 미작성** | 현재는 UserBriefDto 재사용 (사장도 user.name + tel만 필요). Phase 5에서 사장 추가 정보 필요 시 별도 DTO |

### 2026-04-29 (Phase 3-A — User 도메인 JPA 전환 + 하이브리드 영구 공존 정책)

| 항목 | 결정 |
|---|---|
| **영속성 정책** | **JPA + MyBatis 하이브리드 영구 공존** — 단순 CRUD는 JPA, 복잡 쿼리(Store/Owner의 검색·정렬·집계)는 MyBatis 유지 |
| **`mybatis-spring-boot-starter`** | **영구 유지** (제거 X) — Phase 3-D Store/Owner는 MyBatis 표현력 우월, 이후 도메인도 case-by-case |
| **BaseEntity 위치** | `mmg-common/entity/BaseEntity.java` — `@MappedSuperclass + @EntityListeners(AuditingEntityListener)` + `created_at` / `updated_at` (LocalDateTime) |
| **User entity BaseEntity 미상속** | user 테이블에 `created_at`/`updated_at` 컬럼 부재 → 옵션 A 채택. ALTER 회피 (DDL 변경 X), Phase 5 admin 본격 구현 시 status 등과 함께 일괄 ALTER 검토 |
| **MyBatis가 Auditing 컬럼을 만지는 경우** | `BaseEntity.@CreatedDate/@LastModifiedDate`는 JPA에서만 자동. 같은 테이블을 MyBatis로 INSERT/UPDATE 시 SQL에 `NOW()` 명시 필요 (예: `<update> SET ..., updated_at = NOW()`) |
| **`birth` DATE ↔ String** | 응답 스펙(`UserGetRes.birth: String`) 동결 → `StringDateConverter` (`AttributeConverter<String, LocalDate>`, `autoApply=false`) + 필드에 `@Convert` 명시. MyBatis 자동 변환과 동일 동작 검증됨 |
| **`rank` 예약어 처리** | `@Column(name = "\`rank\`")` 백틱 명시. Hibernate가 dialect-specific 인용 처리, MariaDB 정상 동작 확인 |
| **`gender` 타입** | DB INT NULL이지만 entity는 기존과 동일 `int` 유지. MyBatis 시절부터 0/null 구분 안 함 → JPA 전환 후 신규 INSERT는 0 (DB NULL → 0 shift, 응답에는 영향 X) |
| **`ddl-auto`** | **`validate` 고정** (절대 update/create 금지). 학원 공유 DB → DDL 변경은 명시 SQL로만 |
| **`open-in-view`** | `false` — Lazy proxy 트랜잭션 밖 사용 차단 (안티패턴 회피) |
| **UserMapper.java + User.xml** | **삭제** (전 메서드 단순 CRUD → Repository 이동). MyBatis starter는 유지 (Phase 3-B 이후 복잡 쿼리 필요 시 재도입) |
| **Internal API JPA 전환** | `findBriefByUserNo`/`findBriefsByUserNos`도 `@Query` constructor expression으로 `UserBriefDto` 직접 반환. Feign 응답 스펙 변경 X 확인 |
| **`@EnableJpaAuditing`** | 각 서비스 main 클래스에 명시 (`AuthApplication`). 서비스별 opt-in 구조 |
| **Phase 3 분할** | 3-A: User+Address (1주) / 3-B: Payment+Cart+LikedStore (1.5주) / 3-C: Order+Review (2주) / 3-D: Store+Owner는 MyBatis 유지 (옵션 A 확정) |
| **응답 스펙 검증 결과** | 9개 endpoint(checkId/join/login/me/getUser/updateUser/internal 단건/batch/404) 100% 동일 — birth 포맷 `"yyyy-MM-dd"`, role/rank ENUM String, dirty checking UPDATE, ResultResponse 래핑 모두 일치 |
