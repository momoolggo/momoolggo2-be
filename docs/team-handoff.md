# 팀원 영역 핸드오프

본인이 발견한 부채 중 **팀원 영역 모듈** 작업이 필요한 항목 박제. 본인 처리 X (CLAUDE.md §6 #12).

> 영역 매트릭스 확정: 2026-05-06 (CLAUDE.md §3 line 91 참조)

> 작성 원칙: 영역(`mmg-auth-service` / `mmg-gateway` 등) 명시. 권장 처리 방향 + 인용 가능한 R1-A 패턴 + 위치(파일:라인) 박제.

---

## 진행 중

(현재 통보 중인 부채)

---

## R1-B 폐기로 핸드오프 (2026-05-06)

> R1-B 5 Step 중 본인 영역 작업 0건. 5건 모두 팀원 영역 또는 영역 미상.
> 폐기 사유: `feedback_verify_spec_assumptions.md` 1건째 사례 + `project_phase5_r1_state.md` "R1-B ❌ 폐기" 섹션.

### 1. Q-Sec — `UserService.signup` 클라이언트 role 무검증

| 항목 | 내용 |
|---|---|
| **영역** | `mmg-auth-service` ❌ 팀원 |
| **위치** | `mmg-auth-service/.../user/UserService.java:50` 직후 |
| **경위** | R1-B 명세 작성 시점 보안 부채 발견 (R1-A 종결 직후, 2026-05-05) |
| **현재 동작** | 클라이언트가 `{"role":"ADMIN"}` POST 시 검증 없이 그대로 INSERT — 위조 가능 |
| **권장 처리** | 화이트리스트 if-throw 패턴 (R1-A `RiderService.invalidVehicleType` 6요소 복제) |
| **R1-A 패턴 인용** | `mmg-rider-service/.../rider/RiderService.java:35-36` (클래스 상수 `ALLOWED_VEHICLE_TYPES`) + line 107-111 (가드 if-throw + `BusinessException(message, HttpStatus.BAD_REQUEST)`) |
| **메시지 형식** | `"role은 CUSTOMER/OWNER/RIDER 중 하나여야 합니다."` |
| **상수 형식** | `private static final Set<String> ALLOWED_SIGNUP_ROLES = Set.of("CUSTOMER", "OWNER", "RIDER");` |
| **테스트 패턴** | UserServiceTest 도메인 일관 — `.extracting("status").isEqualTo(HttpStatus.BAD_REQUEST)` + `.hasMessageContaining(...)` + verify(never) 5건 + verifyNoInteractions(refreshTokenStore) |

### 2. Q-Timeout — gateway timeout 미설정

| 항목 | 내용 |
|---|---|
| **영역** | `mmg-gateway` ❌ 팀원 |
| **위치** | `mmg-gateway/src/main/resources/application.yml` |
| **경위** | Phase 4-A Feign timeout 패턴 일관 검토 시 발견 |
| **현재 동작** | 모든 라우트 timeout 0 — 백엔드 다운 시 60s 기본 (시한폭탄) |
| **권장 처리** | 모든 라우트 일괄 (connect 3s / read 5s) — Phase 4-A Feign 패턴 일관 |
| **참조 부채** | `docs/tech-debt.md` "mmg-gateway → 백엔드 timeout 설정 부재" 항목 |

### 3. Q-W11 — `InternalUserController.getUsers` empty 분기 통합 테스트

| 항목 | 내용 |
|---|---|
| **영역** | `mmg-auth-service` ❌ 팀원 |
| **위치** | `mmg-auth-service/.../auth/internal/InternalUserControllerIntegrationTest` (신규 케이스 추가) |
| **경위** | Phase 4-A W-2 후속, 통합 테스트 커버리지 갭 |
| **현재 동작** | `getUsers` 코드 정상 (early return), 단 통합 테스트 0건 |
| **권장 처리** | `GET /internal/auth/users?ids=` → 200 + `[]` 통합 테스트 추가 |
| **참조 부채** | `docs/tech-debt.md` "getUsers `?ids=` empty 분기 통합 테스트 미포함" 항목 |

### 4. Phase 4-B Warning 1 — `GatewayIntegrationTest.nonInternalPath_notBlockedByInternalBlock` lambda 단순화

| 항목 | 내용 |
|---|---|
| **영역** | `mmg-gateway` ❌ 팀원 |
| **위치** | `mmg-gateway/.../GatewayIntegrationTest.java:99-111` |
| **경위** | Phase 4-B reviewer Warning 잔존 (cosmetic) |
| **권장 처리** | lambda → `status().is(not(403))` 또는 AssertJ |
| **참조 부채** | `docs/tech-debt.md` "GatewayIntegrationTest cosmetic 2건" 항목 |

### 5. Phase 4-B Warning 2 — `GatewayIntegrationTest` 클래스 Javadoc 잔존 주석

| 항목 | 내용 |
|---|---|
| **영역** | `mmg-gateway` ❌ 팀원 |
| **위치** | `mmg-gateway/.../GatewayIntegrationTest.java:40-41` |
| **경위** | Phase 4-B reviewer Warning 잔존 (cosmetic) |
| **현재 상태** | `"Step 4·5 추가 예정"` 잔존 주석 (Step 4·5 이미 구현됨) |
| **권장 처리** | 주석 제거 또는 `"구현됨"` 정정 |
| **참조 부채** | `docs/tech-debt.md` "GatewayIntegrationTest cosmetic 2건" 항목 |

---

## 6. R6 reject riderNo snapshot — Main 측 해석 주의 (2026-05-11 신규, R6 reviewer S-2)

| 항목 | 내용 |
|---|---|
| **위치** | rider-service `RiderOrderController.notifyMain` → main-service `PUT /internal/order/{orderId}/delivery-status` |
| **현상** | rider 측에서 reject 처리 시 `Delivery.unassignRider()`로 `rider_no = NULL` 변경. 그러나 Main에 보내는 Feign body의 `DeliveryStatusUpdateReq.riderNo`는 **unassignRider 호출 *전* snapshot riderNo** (예: 5L). |
| **의도** | `riderNo` 필드는 *어떤 라이더가 reject 했는지* 감사/추적용. Main 측이 `orders.rider_no`를 갱신할 때 사용 X. |
| **위험** | Main 개발자가 `orders.rider_no = req.riderNo()` UPDATE 시 → rider DB(NULL) ↔ main DB(5L) **불일치** 발생. 다음 배차 시 라이더 매칭 오류 직전. |
| **권장 처리 (main 측)** | `delivery-status` endpoint 처리 시 status가 `WAITING_ASSIGN`이면 `orders.rider_no = NULL` 처리 + body의 `riderNo`는 로그/감사만 사용. 다른 status는 기존 로직 그대로. |
| **참조** | `interfaces.md` §2.1 / `RiderOrderController.notifyMain` / `DeliveryService.rejectDelivery` |

---

## 7. R6-cancel 신규 endpoint — Main 측 처리 (2026-05-11 신규)

| 항목 | 내용 |
|---|---|
| **endpoint** | `POST /api/rider/order/{deliveryNo}/cancel` (rider 측, RIDER role) |
| **호출 시점** | 라이더가 진행 중 배달(ARRIVED_AT_STORE/AWAITING_PICKUP/PICKED_UP/DELIVERING)에서 사고/개인사유/기타로 반려 |
| **rider DB 처리** | delivery.status → WAITING_ASSIGN + rider_no NULL + delivery_log.reason 박제 (ACCIDENT/PERSONAL/OTHER) |
| **Main 동기화 (decision-#35 (가))** | 기존 `PUT /internal/order/{orderId}/delivery-status` 재사용. status=WAITING_ASSIGN 알림만, **reason 정보 전달 X** (rider 도메인 단독 책임) |
| **Main 측 권장 처리** | reject와 동일 — `orders.rider_no = NULL` + status WAITING_ASSIGN 매핑 (`orders.delivery_state = 1` 배달전). riderNo 필드는 감사 추적용. §6 항목과 동일 처리 가능 |
| **reason 활용** | rider 도메인 단독. 향후 R7 정산 / R8 라이더 평가 시 활용 (Main 의존 X). 통계는 rider-service에서 집계 |
| **참조** | `RiderOrderController.cancel` / `DeliveryService.cancelDelivery` / `DeliveryCancelReason` enum |

---

## 8. main → rider 자동 배차 트리거 미구현 (2026-05-12 발견)

| 항목 | 내용 |
|---|---|
| **현상** | `RiderFeignClient.assignRider` (main-service Feign interface, `b1b7f63` KYL) 박제됐으나 **main 코드 내 호출처 0건**. 즉 주문이 들어와도 라이더 측 `delivery` 테이블 INSERT 0 → 라이더 대기 탭 빈 목록. |
| **본인 의도 흐름** | 결제 → `orders.state=1`(대기) → 사장 "주문 수락" → `orders.state=3`(조리중) → **이 시점에 `RiderFeignClient.assignRider()` 호출** → 라이더 측 delivery 생성 → 라이더 화면에 노출 |
| **영역** | `mmg-main-service` ❌ 팀원 (`OrderService` / 가게 측 주문 수락 endpoint 위치). 호출처 추가 = main 책임 |
| **권장 처리 (main 측)** | 1. 가게 "주문 수락" endpoint (또는 state=3 전환 메서드)에 `riderFeignClient.assignRider(req)` 호출 추가. 2. RiderAssignReq 구성 (orderId / storeNo / storeAddress / storeLat/Lng / storePhone / deliveryAddress / deliveryLat/Lng / customerPhone / baseFee). 3. best-effort 패턴 (try-catch + 로그) — 라이더 측 응답 실패해도 orders state 전환은 성공. 4. 라이더 매칭은 라이더 측 R6 `GET /api/rider/order/waiting` (전체 풀에서 선착순) — main이 특정 riderNo 지정 X. |
| **참조** | `RiderFeignClient.java:14-17` / `RiderInternalController.assign` / `DeliveryService.assignDelivery` / CLAUDE.md §7 주문 상태 흐름 |

> **2026-05-12 현재 시연 한계**: Q1-동작 (다) admin 수동 호출 = `POST /internal/rider/{riderNo}/assign` 직접 호출하면 라이더 화면에 즉시 노출. main 측 자동 트리거가 처리되기 전까지의 임시 시연 경로.

---

## 9. R7 정산 admin 측 호출처 미구현 (2026-05-12 신규)

| 항목 | 내용 |
|---|---|
| **endpoint (rider 측, 추가됨)** | `POST /internal/rider/settlement/calculate` (주간 집계 트리거, 멱등) / `POST /internal/rider/settlement/{settlementNo}/confirm` (PENDING → CONFIRMED) / `GET /internal/rider/settlement/pending` (Admin 모니터) |
| **현상** | rider 측 3 endpoint 박제됐으나 **admin-service 내 호출처 0건**. admin 화면에서 "이번 주 정산 집계" 버튼 클릭 / "confirm" 버튼 클릭 시점에 호출 필요. |
| **본인 의도 흐름 (ADR-007 line 98-119 박제)** | 매주 월요일 새벽 (or 임의 시점) admin 화면 → POST /internal/rider/settlement/calculate (periodStart/periodEnd) → 전체 라이더 settlement INSERT (status=PENDING). admin 검토 후 → POST /internal/rider/settlement/{id}/confirm (adminNo) → CONFIRMED 전환. |
| **영역** | `mmg-admin-service` ❌ 팀원 (admin Feign interface 신설 + admin 화면 endpoint에서 호출). 호출처 추가 = admin 책임. |
| **권장 처리 (admin 측)** | 1. `AdminRiderFeignClient` 신설 (또는 기존 `RiderFeignClient` 확장) — `calculateSettlement(CalculateReq)` / `confirmSettlement(Long, ConfirmReq)` / `pendingSettlements()` 메서드. 2. admin Settlement 모니터 화면 endpoint에서 호출. 3. best-effort 패턴 또는 명시 실패 처리 (정산은 멱등 ✅ 재호출 안전). 4. adminNo는 X-Admin-No 헤더 또는 SecurityContext에서 추출. |
| **산출 공식 박제 (ADR-007 line 88-93)** | gross = totalBaseFee + totalExtraFee / commission = gross × 0.10 / tax = (gross - commission) × 0.033 / insurance = 5000원/주 (운행 0건 시 0) / payout = gross - commission - tax - insurance (음수 차단 max 0). Q-Commission-Value (가) 채택. |
| **참조** | `RiderInternalController.java:111-127` / `SettlementService.calculate/confirm/findPending` / `docs/adr/rider/ADR-007-settlement.md` |

---

## 처리 완료

(팀원 처리 완료 시 이력 박제 — 항목 / 처리 커밋 / 처리일)
