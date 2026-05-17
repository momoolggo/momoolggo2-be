# 기술 부채 추적

> 라이더(Phase 5) 작업 중 발견되는 부채를 추적.
> 각 항목은 발견일 / 위치(파일:라인) / 처리 시점 / 책임자 / 근거 커밋을 명시.

---

## 진행 중
(현재 작업 중인 부채)

---

## 백필 대기

### 라이더 진입 전 잔존 부채

| 항목 | 발견일 | 위치 | 처리 시점 |
|---|---|---|---|
| **MapConfigController 네이버 지도 client-id 인증 없이 공개** (`/api/map/key`) | 2026-04-29 | `MapConfigController` | 프론트(`momoolggo-fe`) 협의 후 토큰 방식 전환 (Phase 2-Backfill-D-bis 또는 Phase 5) |

### Phase 5 예정 (TossPaymentClient 분리 시)

| 항목 | 발견일 | 위치 | 처리 시점 |
|---|---|---|---|
| **PaymentService.confirmPayment `@Transactional` 안 HTTP 외부 호출 + timeout 미설정** | 2026-04-29 | `PaymentService.callTossConfirm` (HttpURLConnection) | Phase 5 — `TossPaymentClient` 컴포넌트 추출 + RestTemplate/WebClient 전환 + 트랜잭션 외부로 호출 분리 |
| **PaymentControllerIntegrationTest 학원 DB row PK 하드코딩** (`ORDER_ID_UNPAID=391775460588723L` 등) | 2026-04-29 | `PaymentControllerIntegrationTest` | Phase 2-Backfill-B — `@Transactional + @Rollback + fixture INSERT` 패턴으로 전환 (다른 5개 테스트 패턴과 통일) |
| **DeliveryService.ALLOWED_TRANSITIONS 공유 맵 — updateStatus가 cancel 4 전이도 통과 (R6-cancel reviewer W-1)** | 2026-05-11 | `DeliveryService.java:55-83` | R7 진입 전 — (A) `updateStatus` 내부 cancel 전이 분기 추가(reason 필수 + unassignRider 강제) 또는 (B) cancel 전이를 별도 Set으로 분리. 현재 `updateStatus` 외부 호출처 0이라 미실현 위험 (admin/system actor 외부 호출 시 reason NULL + rider_no 잔존 위험). |

---

## 예정된 작업

> 단순 백필이 아니라 **신규 코드 변경 + 컨트롤러 시그니처 변경 + 프론트 영향**이 동반되는 항목.

### Phase 5 또는 후속 단계

| 항목 | 발견일 | 위치 | 처리 방향 |
|---|---|---|---|
| **MapConfigController 네이버 client-id 공개 보안 부채** (`/api/map/key`) | 2026-04-29 | `MapConfigController` | 프론트 협의 후 처리 — Phase 5 |
| **OwnerStoreUpdateReq.storeId 타입 정리 (String → Long)** | 2026-04-30 | `OwnerStoreUpdateReq.java:9` `private String storeId` | D-bis에서 임시로 `Long.parseLong` 변환 + BusinessException BAD_REQUEST 처리. 근본 해결은 dto 타입 변경이지만 프론트(`StoreManagementView.vue` 등)의 storeId 전송 형식 협의 필요. |
| **OwnerService.uploadImage 확장자 화이트리스트 부재** | 2026-04-30 | `OwnerService.uploadImage` (39행) — `contentType.startsWith("image/")`만 체크 | content-type 헤더 위조 시 우회 가능. 실 확장자(jpg/png/gif/webp 등) 화이트리스트 추가 필요. multipart 10MB 제한은 적용됨. |
| **OwnerService.getOrders Feign 예외 fallback 미정 (W-2 후속 — 의미 분리)** | 2026-04-30 | `OwnerService.getOrders` — `authFeignClient.getUsers(userNos)` 예외 시 그대로 propagate → 점주 화면에 500 노출 | Phase 5 라이더 정리 또는 본격 작업 중 처리 — `TossPaymentClient` 분리 작업과 함께 Feign Fallback 패턴 도입 (CircuitBreaker / 빈 응답 fallback). 비즈니스 결정 필요 (점주가 주문 목록 자체를 못 보는 게 맞나 vs 부분 정보로라도 보여야 하나). 단위 테스트 `feignException_propagates`가 현재 동작 명시 동결. **Phase 4-A 백필(2026-05-02)에서 "null 가드"와 의미 분리** — null 가드는 별도 항목으로 즉시 처리됨. |
| **getUsers `?ids=` empty 분기 통합 테스트 미포함** | 2026-05-02 | `InternalUserController.getUsers` 41행 `if (userNos == null || userNos.isEmpty()) return List.of()` | Phase 5 라이더 진입 전 또는 Internal API 수정 시점 보완. 코드는 정상(early return), 테스트 커버리지 갭만. consumer 측(OwnerServiceTest empty orders) 단위로 간접 커버. Spring MVC `?ids=` 빈 query string 처리 동작이 불확실해 진단 시 의도적 제외. |
| **BaseSecurityConfig CORS allowedMethods PATCH 누락 (이중화 부분 깨짐)** | 2026-05-02 | `BaseSecurityConfig` (mmg-common) — Gateway는 GET/POST/PUT/PATCH/DELETE/OPTIONS 6개, 각 서비스는 PATCH 없음 | 이중화 제거 작업 (CLAUDE.md "서비스 CORS 제거는 Phase 5 검토" 명시) 시 함께 처리. 외부 Gateway 경유 요청은 영향 0 (Gateway 우선 처리). 로컬 개발 중 서비스 직접 호출 시 PATCH 거부 가능성. tech-debt 한 항목 = 한 개념 (4-A W-2 교훈) — "PATCH 누락" 단일 부채로 등재. |
| **mmg-gateway 라우트 URL 하드코딩 (`http://localhost:{port}` 12곳)** | 2026-05-02 | `mmg-gateway/src/main/resources/application.yml` routes 전체 | Phase 5 배포 준비 시점 — Docker/K8s 환경에서 서비스 hostname env화 필요. 학원 발표는 로컬 환경이라 현재 블로커 없음. |
| **mmg-gateway → 백엔드 timeout 설정 부재** | 2026-05-02 | `mmg-gateway/.../application.yml` — `spring.cloud.gateway.server.webmvc.routes[].timeout` 또는 httpclient 레벨 미설정 | Phase 5 rider/admin 서비스 기동 시 처리. 현재 rider(8082)/admin(8083) 미기동이라 connection refused로 즉시 반환 — 실질 무한대기 없음. |
| **GatewayIntegrationTest cosmetic 2건 (W-1 nonInternalPath lambda 단순화 + W-2 클래스 Javadoc 'Step 4·5 추가 예정' 잔존)** | 2026-05-02 | `GatewayIntegrationTest.java:104-111, 40-41` | Phase 4-B reviewer Warning — 머지 차단 아님. 다음 GatewayIntegrationTest.java 손볼 때(Phase 5 라이더 라우트 활성화 시점 등) 정리. |
| **AT/RT 만료 운영값 미적용 (15일/15일 개발용 유지)** | 2026-04-28 | `.env.example` `JWT_ACCESS_TOKEN_VALIDITY_MS=1296000000` / `JWT_REFRESH_TOKEN_VALIDITY_MS=1296000000` (CLAUDE.md §2 명시) | 학원 발표 후 또는 운영 전환 시점 — 30분/14일로 변경. Phase 1 JwtTokenProviderTest 영향 점검 필요 (만료 로직 검증 케이스). Q3 (a) 그대로 유지 결정 (4-C 백필과 직접 연관 없음). |
| **Phase 4-C 의도 제외분 — 날씨 캐시 (Redis 캐시 + 기상청 API)** | 2026-05-02 | (구현 안 됨, CLAUDE.md §7 `weather:grid:{nx}:{ny}` TTL 1시간 명시) | Phase 5 펫 Lv.10 추천 도입 시 함께. 사용처 부재 시 인프라만 도입 = dead config (NAJACKS 변종) — Q1 옵션 A 결정. |
| **Phase 4-C 의도 제외분 — Pub-Sub 인프라 (Redis Pub/Sub)** | 2026-05-02 | (구현 안 됨, CLAUDE.md §2 명시) | Phase 5 라이더 정리 ADR에서 통신 패턴(Feign vs Pub-Sub) 결정 후 사용처와 함께. 후보: 라이더 위치 broadcast / 챗봇 ESCALATED 알림 / 주문 상태 변경. |
| **다중 디바이스 RT 저장 (Phase 5 검토)** | 2026-05-02 | `RedisRefreshTokenStore` key `rt:{userNo}` 단일 — 재로그인 시 덮어쓰기 (모바일/PC 동시 로그인 시 한쪽 강제 logout) | Phase 5 — 다중 디바이스 요구 시 key를 `rt:{userNo}:{jti}` 또는 `rt:{userNo}:{deviceId}` 형태로 확장. 현재는 단일 디바이스 가정. |
| **AT blacklist (Phase 6 보안 강화)** | 2026-05-02 | (구현 안 됨, Phase 4-C Q2 (a) RT만 결정) | Phase 6 — logout 즉시성 요구(현재 AT는 만료 전 사용 가능) 또는 위조 차단. AT blacklist 키 `at_blacklist:{jti}` 형태. JWT jti claim 도입 동반 필요. |
| **RefreshTokenStore mmg-common 이관 (Phase 5 라이더/admin RT)** | 2026-05-02 | `mmg-auth-service/.../token/` 위치 (D3 결정) | Phase 5 — 라이더/admin이 RT 다룰 필요 발생 시 mmg-common으로 이관. 현재는 auth만 사용 (YAGNI). |
| **Phase 4-C reviewer Warning 3건 (cosmetic, 발표 전 확인 권장)** | 2026-05-02 | `UserService.issueAndStoreTokens` 쿠키-Redis 순서 / `UserServiceTest` reissue 메시지 부분 일치 / `docker-compose.yml` Redis 비밀번호 부재 | W-1: 학원 발표 전 리허설(Redis 강제 kill 시 login 시도 시나리오)로 Servlet spec 동작 확인 권장. W-2: 메시지 변경 시 silent 통과 위험 — 다음 reissue 손볼 때 정확 검증으로 갱신. W-3: 개발 환경 무방, 운영 전환 시 `--requirepass` 추가 검토. |
| **accountNo 평문 노출 (RiderProfileRes 본인 한정 응답)** | 2026-05-05 | `mmg-rider-service` R1-A — `RiderProfileRes.accountNo` (GET `/api/rider/me`) | Phase 6+ 마스킹 — D7 손님 전화번호 마스킹 패턴 일관(예: 앞 N자리만 노출, 나머지 `*`). 본인 한정 응답이라 즉시 보안 위험은 낮음(`@PreAuthorize` hasRole(RIDER) + 본인 user_no lookup). 외부 노출(관리자/사장 화면) 추가 시점에 재검토. |
| **WorkSessionService toggleStatus race condition → 중복 진행 세션 → 다음 조회 500 (R8 W-2)** | 2026-05-12 | `WorkSessionService.toggleStatus` — `findByRiderNoAndEndedAtIsNull` empty 체크 후 `save` 패턴 (line 118) | Phase 6+ — 동시 토글 요청 시 중복 INSERT 가능. 다음 `findByRiderNoAndEndedAtIsNull` 호출이 `IncorrectResultSizeDataAccessException` → HTTP 500. ADR-008 line 193 미해결 항목과 동일 (Q-Break (가) 메모리 측정 MVP 채택 한계). 처리 옵션: (A) DB unique 제약 `(rider_no, ended_at IS NULL)` partial index — MySQL 미지원, 회피 필요 (B) Redis 락 `lock:work-session:{riderNo}` (C) `rider` 행 `@Version` 낙관적 락 + 토글 흐름 보호. 운영 진입 시 (B) 또는 (C) 채택. |
| **R8 reviewer Cosmetic — `getSummary` week = 최근 7일 rolling window (월~일 아님)** | 2026-05-12 | `WorkSessionService.getSummary` line 194 — `LocalDate.now().minusDays(6).atStartOfDay()` | Phase 6+ UI 협의 — 기획 요구사항 "이번 주 합계"가 월~일(주 단위) vs 최근 7일(rolling) 모호. 현재 rolling 채택. FE 화면 텍스트 "최근 7일"로 레이블링 권장. |
| **R9 reviewer W-2 — timezone 암묵 의존 (LocalDate.now() / RIDER_DB_URL serverTimezone 미명시)** | 2026-05-12 | `DeliveryService.getMyCompletedDeliveries` line 337/346 + `RIDER_DB_URL` (.env) + JVM 기본 timezone | 학원 공유 환경(동일 기계)은 무해. 배포 환경 이전 전 처리: (i) `RIDER_DB_URL` 끝에 `?serverTimezone=Asia/Seoul` 추가, (ii) `spring.jpa.properties.hibernate.jdbc.time_zone=Asia/Seoul` 또는 JVM `-Duser.timezone=Asia/Seoul`. DB server timezone과 JVM timezone 불일치 시 기간 필터 경계가 수 시간씩 밀릴 위험. |
| **AddressSearchService silent fail — 모든 외부 호출이 `try-catch + log.warn + 빈 리스트 반환`으로 NCP 401/rate limit/네트워크 에러를 사용자에게 "결과 없음"으로 은폐** | 2026-05-16 | `AddressSearchService.java:79-82, 116-119, 181-184` (`searchByLocal` / `searchByGeocoding` / `reverseGeocode` 3곳) | Phase 6+ 또는 4-C D1 throw 패턴 정리 시 — `log.warn` → `log.error` 격상 + `BusinessException(5xx)` 또는 `BusinessException(BAD_GATEWAY)` throw로 변환해 GlobalExceptionHandler가 사용자에게 명시 응답. Phase 4-C `RedisRefreshTokenStore` D1 시작점 정합성 throw 패턴 일관. silent fail은 진단 어려움 누적 — 본 부채로 작업 B 진단에서 H1/H2 가설 분리에 30분+ 소비. 별개 트랙 정리 권장 (W-2 한 항목 = 한 개념). |
| **`/api/address/**` rate limit 부재 — 비로그인 permitAll 노출이라 NCP 무료 quota 소진 위험** | 2026-05-16 | `MainSecurityConfig.java:28-30` (작업 B에서 `/api/address/**` GET permitAll + `/api/map/**` GET permitAll 추가) + NCP Maps Geocoding/Reverse 호출 (`AddressSearchService`) | Phase 6+ — (A) Bucket4j 도입 (Service 메서드 레벨, IP/userNo당 1분 N건) 또는 (B) Gateway 글로벌 RateLimiter 필터 (Spring Cloud Gateway 기본 redis-rate-limiter). dead config 회피 원칙 — 사용처(NCP 호출량 모니터링/quota 초과 이력) 발생 시 도입. 학원 발표 단계는 MVP 단순성 유지 (Q-B3 결정 (다)). |
| **주문번호 UI zero-pad + display suffix formatter (Figma 정정 9 "000001A" 표기 의도)** | 2026-05-16 | DB는 Long (`orders.order_id BIGINT AUTO_INCREMENT` + `delivery.order_id BIGINT` case-#34 정정). Figma는 zero-pad 6자리 + 임의 alphabet suffix 표기 (예: 1L → "000001A"). FE 표시 layer에서 변환 의도, BE 응답 Long 그대로 | Phase 6+ — FE에서 표시 시점 변환 (Vue computed 또는 filter). BE는 Long 박제 일관 유지. case-#34 박제 일관 (`feedback_verify_diagnostic_assumptions.md`) — Figma 박제는 UI mockup 자료, ID 체계 결정 자리 X. R2-a 시점 의도 보존하되 main DB BIGINT 기준 정합. |
| **case-#33 interfaces.md §3.3/§3.4 path 차이 (R7 정정 후 interfaces.md 후속 정정 누락)** | 2026-05-16 | `docs/adr/rider/interfaces.md` §3.3 line 211 (`POST /internal/settlement/{settlementId}/confirm`) + §3.4 line 232 (`GET /internal/settlement/calculate?periodStart=&periodEnd=`) vs 실제 rider `RiderInternalController.java:117/123` (`POST /internal/rider/settlement/calculate` + `POST /internal/rider/settlement/{settlementNo}/confirm`). + §2.3 `/internal/files/upload` 박제만 잔존 (Q-A5 (나) — 사용처 부재로 미구현) | 별 정정 트랙 — 작업 A는 rider Provider 박제 우선 적용(Q-A2 (다)). interfaces.md 정정은 다음 ADR 갱신 시점 또는 학원 발표 후 일괄. `feedback_verify_diagnostic_assumptions.md` case-#33 박제 일관. |
| **mmg-main `RiderFeignClient` 단위 테스트 0건 (`assignRider` / `getRiderLocation` / `getRiderStatus`)** | 2026-05-16 | `mmg-main-service/.../feign/RiderFeignClient.java` + 단위 테스트 0 grep 결과 | Phase 6+ 또는 admin AuthFeignClient/MainFeignClient 단위 테스트 패턴 도입 시점 — WireMock + `@FeignClient` Spring 컨텍스트 통합 1건 추천 (4-A 패턴 일관). 학원 발표 단계 무관, 작업 A 시점 발견 (Q-A8 사전 검증 부산). |
| **`orders.completed_at` 컬럼 부재 — `InternalOrderController.completeDelivery` body `completedAt` 수신 후 무시 중** | 2026-05-16 | `mmg-main-service/.../order/OrderService.completeDelivery` line `log.info(... — orders.completed_at 컬럼 부재로 무시)` + `mmg-main-service/.../internal/dto/DeliveryCompleteReq.completedAt` + interfaces.md §2.2 body 박제 | Phase 6+ analytics 도메인 — (A) `ALTER TABLE orders ADD COLUMN completed_at DATETIME NULL` 학원 DB + `Orders.completedAt` @Column 추가 + `OrderService.completeDelivery` `order.setCompletedAt(completedAt)` 활성화. Q-A8.e-1 (나) 결정 일관 — 작업 A α 범위 좁힘, analytics 사용처 발생 시 도입. |
| **`orders` 테이블 4 컬럼 부재 — interfaces.md §1.1 박제 13 필드 vs main DB 실측 차이 (case-#34-후속)** | 2026-05-17 | `mmg-main-service/src/main/resources/mappers/Owner.xml` `findStoreInfoByOrderId` SELECT — `NULL AS deliveryLat / deliveryLng / customerPhone` + `0 AS extraFee` 패스. main DB `orders` 13 컬럼 (DESCRIBE) vs interfaces.md §1.1 13 필드 박제. 부재: `delivery_lat / delivery_lng / customer_phone / extra_fee` | Phase 6+ — (A) `ALTER TABLE orders ADD COLUMN (delivery_lat DECIMAL(16,13), delivery_lng DECIMAL(16,13), customer_phone VARCHAR(20), extra_fee INT DEFAULT 0)` 학원 DB + `Orders` entity 4 필드 추가 + 기존 ~452행 backfill 결정 (auth user.tel 동적 보강 vs NULL 잔존) + customerPhone D7-a 평문 박제 정정 (현재 NULL = D7-a 미적용). Q-A9.e (나) 결정 일관 — 작업 A α 범위 좁힘 (R4 박제 실호출 마무리, 자료 박제 별 트랙). dead config 회피 (사용처 명확 시 도입). |

---

## 해결 완료

| 항목 | 발견일 | 처리일 | 커밋 |
|---|---|---|---|
| reissue JwtException 500 응답 (RT 만료가 서버 오류로 둔갑) | 2026-04-29 | 2026-04-29 | `550e824` (Phase 1 백필) |
| UserUpdateReq.gender `int` (미전송과 0 구분 불가 → 변경 무시) | 2026-04-29 | 2026-04-29 | `3b06047` (Phase 1 백필) |
| 조회 메서드 `@Transactional(readOnly=true)` 누락 | 2026-04-29 | 2026-04-29 | `3e28474` (Phase 1 백필) |
| **calSumOrder가 orderId 받음 → deleteOrder 후 서브쿼리 empty로 store.order_count 미갱신** | 2026-04-29 | 2026-04-29 | `6d75c61` `e500e60` (Phase 2-A) |
| **PaymentService.confirmPayment 흐름이 거꾸로** (장바구니 정리가 결제 저장보다 먼저) | 2026-04-29 | 2026-04-29 | `6565841` `c3f24d5` (Phase 2-A) |
| `order-delete-not-found.json` snapshot 임시 문자열 `"ㅇㅇ"` 동결 | 2026-04-29 | 2026-04-29 | `6e1aa68` (Phase 2-A) |
| **ReviewService 예외 케이스 (403 주문자 불일치 / 409 중복 리뷰) 테스트 0** | 2026-04-29 | 2026-04-30 | `ba284a7` (Phase 2-Backfill-C) |
| **CartService.clearAndAddToCart / deleteCartItem 단위 테스트 0** | 2026-04-29 | 2026-04-30 | `f6a15ab` (Phase 2-Backfill-C — 현재 동작 동결, 권한 분기는 D로 이관) |
| **UserAddressService.delete addressId 유효성 검증 없음** | 2026-04-29 | 2026-04-30 | `d21834d` (Phase 2-Backfill-C — 현재 동작 동결, 권한 분기는 D로 이관) |
| **StoreService.storeOneGet Feign null NPE** | 2026-04-29 | 2026-04-30 | `2102bb5` `bdb6c6e` (Phase 2-Backfill-D — BusinessException NOT_FOUND 처리 + 5 케이스 단위 테스트) |
| **AddressSearchService `new RestTemplate()` 매 요청 + timeout 미설정** | 2026-04-29 | 2026-04-30 | `fb3b021` `b658378` (Phase 2-Backfill-D — RestTemplate 싱글톤 Bean + connect 3s/read 5s + 8 케이스 테스트) |
| **CartService 권한 분기 추가 (cartItem 소유자 검증)** | 2026-04-30 | 2026-04-30 | `f35d1c9` `f4b810b` (Phase 2-Backfill-D — Service/Controller 시그니처 변경 + 단위 5 + 통합 4) |
| **UserAddressService.delete 권한 분기 추가 (userNo 파라미터)** | 2026-04-30 | 2026-04-30 | `890e3ad` `5621730` (Phase 2-Backfill-D — Service/Controller 시그니처 변경 + 단위 4) |
| **CartService UPDATE 회로 통합 테스트 추가 (Warning 1)** | 2026-04-30 | 2026-04-30 | `f4b810b` (Phase 2-Backfill-D — `CartIntegrationTest`로 dirty checking + 권한 + 롤백 안전성 검증) |
| **CartIntegrationTest 1차 캐시 의존 강화 (Warning)** | 2026-04-30 | 2026-04-30 | `ef77f34` (Phase 2-Backfill-D — entityManager.clear() 추가 → DB SELECT 검증 격상) |
| **OwnerService 17개 메서드 권한 분기 일괄 추가** | 2026-04-30 | 2026-04-30 | Phase 2-Backfill-D-bis — Mapper 헬퍼 4개 + Service verify 4개 + 5 그룹 (가게/주문/메뉴/매출/카테고리). 커밋: `a0ba8a2`(인프라) + 그룹별 feat 5 + test 5. 신규 32 케이스, 148/148 PASS. |
| **registerStore dto.userId 위조 방지** | 2026-04-30 | 2026-04-30 | `aa65c86` `8963d58` (Phase 2-Backfill-D-bis 그룹 ㄱ) — 옵션 B (불일치 시 FORBIDDEN throw) |
| **OwnerController.updateStoreStatus 응답 null (W-1, 기존 부채)** | 2026-04-30 | 2026-04-30 | `1cf07a7` (Phase 2-Backfill-D-bis 후처리) — Service 결과 받아놓고 응답에 `null` 반환하던 버그. `null` → `updatedStore` 1줄 수정. Phase 2-B에서 도입된 부채. |
| **OrderController.deleteOrder 인증 누락 (Critical 1)** | 2026-04-30 | 2026-04-30 | `08a4a28` `e57587b` (Phase 3-Backfill-A-1) — `@AuthenticationPrincipal` 추가 + 소유자 검증. 응답 스펙 동결 (미존재 → return 0). |
| **StoreController FavoriteToggle dto.userNo 위조 (Critical 3, D-bis 패턴 재발)** | 2026-04-30 | 2026-04-30 | `6a284c0` `6979ce4` (Phase 3-Backfill-A-2) — wishToggle/checkWish/wishListGet 3곳 옵션 B 적용. System.out 1건 제거. |
| **OrderController 내역 엔드포인트 인증 누락 (Critical 2)** | 2026-04-30 | 2026-04-30 | `54a267b` `9ebc82f` (Phase 3-Backfill-A-3) — getOrderHistory/orderHistoryDetail/maxHistoryPage 3곳. System.out 1건 제거. |
| **OrderService.getOrderInfo Feign null NPE (Critical 4)** | 2026-04-30 | 2026-04-30 | `6d6cc14` `2011b90` (Phase 3-Backfill-A-4) — storeOneGet 패턴 전파. BusinessException NOT_FOUND. |
| **StoreService.getStoreReviews Feign batch null NPE (Major)** | 2026-04-30 | 2026-04-30 | `361ce00` `2011b90` (Phase 3-Backfill-A-4) — null 응답 → 빈 Map → userName 빈 문자열 fallback. Owner.getOrders와 다른 결정(Phase 5). |
| **UserAddressService.update 소유자 검증 누락 (Major)** | 2026-04-30 | 2026-04-30 | `5253bef` `ca78b7a` (Phase 3-Backfill-A-5) — D-4 delete 패턴을 update에도 일관 적용. 4 신규 케이스. |
| **권한 비교 패턴 비일관 (Long != long / .equals() / null 가드 혼용, W-A1 6곳)** | 2026-04-30 | 2026-05-02 | `e95cf97` `ef9a097` `aefb576` (W-A1) — Order/Cart/UserAddress 6곳 `Objects.equals()` 단일 패턴 통일 + null 가드 redundant 제거. 표준 `feedback_owner_check_pattern.md`. |
| **잔존 primitive `long != long` 비교 10곳 통일 (W-A1 후속 — Phase 3-Backfill-C)** | 2026-05-02 | 2026-05-02 | `682a240` `a136582` `8a62d48` (Phase 3-Backfill-C-1) — Owner 5 + Store 3 + Order 2 = 10곳 `Objects.equals()`. autoboxing 동작 동일 (위험 0). W-A1 시점 결정 근거 미명시였던 부분 정정 — primitive 비교도 단일 표준 명시. |
| **StoreService.storeSearchList null/blank 입력 검증 단위 부재** | 2026-05-02 | 2026-05-02 | `fe8db01` (Phase 3-Backfill-C-2) — verifyNoInteractions으로 Mapper 미호출 동결 2건. |
| **StoreService.wishToggle delete 분기 단위 부재 (insert만 단위 커버)** | 2026-05-02 | 2026-05-02 | `eced681` (Phase 3-Backfill-C-3) — deleteByUserNoAndStoreId verify + saveAndFlush 미호출 1건. toggle 양방향 단위 완성. |
| **OwnerService.getOrders Feign null 가드 누락 (A-4 패턴 전파 누락 — W-2와 의미 분리 후 즉시 처리)** | 2026-04-30 | 2026-05-02 | `d2f87ce` `39e43cd` (Phase 4-A 백필) — StoreService.getStoreReviews 패턴 복사 (null → 빈 Map → customerName/tel 미설정 fallback). W-2의 "예외 fallback"과 별개 결정으로 분리 처리. 단위 테스트 `feignNull_customerFieldsNotSet`로 동결. |
| **InternalUserController 통합 테스트 0건 (Feign provider 동결 부재)** | 2026-04-28 | 2026-05-02 | `b562532` (Phase 4-A 백필) — `@SpringBootTest+MockMvc+@Transactional+@Rollback+fixture INSERT` 6건. auth-service 첫 실 DB 통합 도입. Feign consumer 4곳이 의존하는 직렬화 형식 + 404 응답 + batch 동작 동결. |
| **UserBriefDto Jackson 직렬화 snapshot 0건 (필드 추가/제거 시 consumer 깨짐 위험)** | 2026-04-28 | 2026-05-02 | `05a2e2b` (Phase 4-A 백필) — JSON 키 4개(userNo/name/tel/address) 동결 + 라운드트립 검증. mmg-common 단위 1건. |
| **InternalUserController 조회 메서드 `@Transactional(readOnly=true)` 누락 (B-1 패턴 잔존)** | 2026-04-28 | 2026-05-02 | `642bd89` (Phase 4-A 백필) — getUser/getUsers/getOwner 3 메서드 일괄 적용. Phase 3-B-1 표준 일관. |
| **Feign client timeout 미설정 (60s 기본값 — auth-service 다운 시 시한폭탄)** | 2026-04-28 | 2026-05-02 | `7aec4d7` (Phase 4-A 백필) — main-service `application.yml`에 `spring.cloud.openfeign.client.config.default.connectTimeout: 3000 / readTimeout: 5000`. RestTemplate(AddressSearchService) 빈 timeout과 통일. auth-service는 openfeign 의존성 부재로 미적용 (dead config 회피). |
| **mmg-gateway HelloController dead endpoint (Phase 0-B 잔재)** | 2026-04-28 | 2026-05-02 | `a4488b8` (Phase 4-B 백필) — Gateway 외부 노출 가능했던 `GET /hello` 디버그 엔드포인트 + `hello/` 디렉토리 정리. admin/rider 동일 패턴은 Phase 5 신규 기능 도입 시 함께. |
| **mmg-gateway 자동화 테스트 0건 (수동 검증만)** | 2026-04-29 | 2026-05-02 | `8af4dfa` `08a226c` `9d92347` (Phase 4-B 백필) — Gateway 첫 통합 테스트 도입 (WebMVC 5.0.1 + `@SpringBootTest+MockMvc.webAppContextSetup+@Autowired CorsFilter+GatewayMvcProperties`). InternalBlock 12 + 라우트 정의 13 + CORS preflight/404 3 = 28 통합 + parseAllowedOrigins 단위 1 = 29건. |
| **GatewayCorsConfig allowedOrigins `.split(',')` trim 누락 (env 공백 시한폭탄)** | 2026-05-02 | 2026-05-02 | `7e87567` `f7f8bf6` (Phase 4-B 백필) — `parseAllowedOrigins(raw)` 정적 메서드 추출 + `map(String::trim) + filter(!empty)`. 단위 6 assertion (단일/콤마뒤공백/연속콤마/빈 입력). 학원 발표 환경변수 사고 차단. |
| **`.env.example` 실제 시크릿 노출 (DB 비번/JWT_SECRET/NAVER_CLIENT_SECRET 등 git 추적)** | 2026-05-02 | 2026-05-02 | `3720d8f` (Phase 4-C 0단계 — Q6 (b) 별도 PR 즉시) — placeholder 교체. 실값은 .env(gitignored)로 이동. git history 시크릿 정리(BFG/filter-repo) + 시크릿 재발급은 별도 결정. |
| **현재 RT 저장소 부재 (RT revoke 불가 보안 이슈, logout이 진짜 logout 아님)** | 2026-04-28 | 2026-05-02 | Phase 4-C 신규 기능 (`693bf84` `3259c28` `c26a4c4` `6fd8ce1` `d903672` 외) — `RefreshTokenStore` 인터페이스 + `RedisRefreshTokenStore` (Lettuce, key `rt:{userNo}`, value RT 자체, TTL JWT 만료시각 동기). signup/signin → 저장, reissue → 비교 검증, signout → 삭제. D1 정합성(login throw) + D1-bis best-effort(signout 쿠키 만료 진행). 신규 단위 10건. |
| **main-service 조회 메서드 `@Transactional(readOnly=true)` 누락 (24건, 6도메인)** | 2026-05-02 | 2026-05-02 | `38cff0b` `3db1076` `8fe7e7b` `3bf15d0` `6024407` `207996d` (B-1) — Review/Store/Order/Cart/UserAddress/Owner 일괄 적용. auth-service 패턴 일관. |
| **OwnerService 쓰기 메서드 `@Transactional` 누락 (8건, 데이터 정합성 부채)** | 2026-05-02 | 2026-05-02 | `11422c3` (B-1 확장) — `registerStore` 3 INSERT 부분 실패 위험 등 8건 일괄 처리. *발견 즉시 처리* (tech-debt 등재 X 결정). |
| **Review 통합 happy path 부재 (post/delete)** | 2026-05-02 | 2026-05-02 | `dca7c02` (B-2) — `ReviewControllerIntegrationTest`에 2건 추가. `entityManager.flush() + clear() + JPQL/findById` 재조회 검증. |
| **UserAddressService.save / setDefault 단위 테스트 부재** | 2026-05-02 | 2026-05-02 | `8ffba23` (B-3) — Save 3건(defaultAd 분기) + SetDefault 2건 추가. 누적 13건 단위 테스트. |
| **StoreController `System.out.println` 잔존 1건** | 2026-05-02 | 2026-05-02 | `ad63030` (B-4) — 디버그 잔재 단순 제거. main-service 내 `System.out` 0건 도달. |
| **DeliveryLog.actorRole String → ActorRole enum 도입 (R2-b → R3-a 처리)** | 2026-05-06 | 2026-05-10 | `e32f8ca` (R3-a) — ActorRole enum 신설 + DeliveryLog 마이그레이션 + DeliveryLogTest 갱신. mmg-rider-service 단독 + mmg-common 미수정. |
| **WorkSession.vehicleType / Rider.vehicleType String → VehicleType enum 도입 (R2-c → R3-a 처리)** | 2026-05-07 | 2026-05-10 | `19edc43` `eb3a9e5` `396a270` (R3-a) — VehicleType enum 신설 + 양 entity 마이그레이션 + RiderService valueOf + 응답 동결 (RiderProfileRes.from() .name() 변환) + RiderServiceTest 6건/WorkSessionTest 3건. |
