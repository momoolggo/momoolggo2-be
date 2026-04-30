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

---

## 예정된 작업

> 단순 백필이 아니라 **신규 코드 변경 + 컨트롤러 시그니처 변경 + 프론트 영향**이 동반되는 항목.

### Phase 2-Backfill-D-bis — Owner 권한 분기 일괄 추가 (라이더 진입 전 처리)

| 항목 | 발견일 | 위치 | 처리 방향 |
|---|---|---|---|
| **OwnerService 14개 메서드 권한 분기 일괄 추가** | 2026-04-30 | `OwnerService` 전체 (registerStore/updateStore/deleteStore/updateStoreStatus/getOrders/updateOrderState/deleteOrder/registerMenu/updateMenu/deleteMenu/매출 2/카테고리 4) — `getMyStore/getMyStores`만 ownerNo 필터, 나머지는 store_id/order_id/menu_id/dto.userId만 받고 점주 본인 소유 검증 X | 모든 메서드에 `long callerOwnerNo` 추가 → `ownerMapper.findStoreOwnerByXxxId(...)` 후 `storeOwner == callerOwnerNo` 검증 → 불일치 시 `BusinessException FORBIDDEN`. 14개 메서드 + 14개 컨트롤러 + dto.userId 위조 방지(`registerStore`) 포함. 프론트 영향 LOW (URL/body 형식 그대로). |
| **`OwnerServiceTest` 권한 분기 부재 동결 → 권한 검증 단위 테스트로 전환** | 2026-04-30 | `OwnerServiceTest` 18 케이스 (D-1-B에서 현재 동작 동결로 작성) | D-bis 권한 분기 추가 시 시그니처 갱신 + 403 케이스 추가. 클래스 주석의 "권한 분기는 D-bis 예정" 문구 제거. |
| **MapConfigController 네이버 client-id 공개 보안 부채** (`/api/map/key`) | 2026-04-29 | `MapConfigController` | 프론트 협의 후 처리 — D-bis 또는 Phase 5 |

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
