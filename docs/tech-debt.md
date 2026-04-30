# 기술 부채 추적

> 라이더(Phase 5) 작업 중 발견되는 부채를 추적.
> 각 항목은 발견일 / 위치(파일:라인) / 처리 시점 / 책임자 / 근거 커밋을 명시.

---

## 진행 중
(현재 작업 중인 부채)

---

## 백필 대기

### Phase 2-Backfill-D 예정

| 항목 | 발견일 | 위치 | 처리 시점 |
|---|---|---|---|
| **MapConfigController 네이버 지도 client-id 인증 없이 공개** (`/api/map/key`) | 2026-04-29 | `MapConfigController` | 프론트(`momoolggo-fe`) 협의 후 토큰 방식 전환 (Phase 2-D 또는 Phase 5) |
| **AddressSearchService `new RestTemplate()` 매 요청 + timeout 미설정** | 2026-04-29 | `AddressSearchService` (searchByLocal/searchByGeocoding/reverseGeocode) | Phase 2-Backfill-D — `@Bean RestTemplate` + connect/read timeout |
| **StoreService.storeOneGet Feign null NPE** | 2026-04-29 | `StoreService.java:32` `authFeignClient.getOwner(...)` | Phase 2-Backfill-D — null 처리 + 단위 테스트 |

### Phase 5 예정 (TossPaymentClient 분리 시)

| 항목 | 발견일 | 위치 | 처리 시점 |
|---|---|---|---|
| **PaymentService.confirmPayment `@Transactional` 안 HTTP 외부 호출 + timeout 미설정** | 2026-04-29 | `PaymentService.callTossConfirm` (HttpURLConnection) | Phase 5 — `TossPaymentClient` 컴포넌트 추출 + RestTemplate/WebClient 전환 + 트랜잭션 외부로 호출 분리 |
| **PaymentControllerIntegrationTest 학원 DB row PK 하드코딩** (`ORDER_ID_UNPAID=391775460588723L` 등) | 2026-04-29 | `PaymentControllerIntegrationTest` | Phase 2-Backfill-B — `@Transactional + @Rollback + fixture INSERT` 패턴으로 전환 (다른 5개 테스트 패턴과 통일) |

---

## 예정된 작업

> 단순 백필이 아니라 **신규 코드 변경 + 컨트롤러 시그니처 변경 + 프론트 영향**이 동반되는 항목.
> Phase 2-Backfill-C에서 진단되어 D 단계로 분리된 작업.

### Phase 2-Backfill-D — 권한 분기 추가 (라이더 진입 전 처리)

| 항목 | 발견일 | 위치 | 처리 방향 |
|---|---|---|---|
| **CartService 권한 분기 추가 (cartItem 소유자 검증)** | 2026-04-30 | `CartService.updateCartItem(Long cartItemId, int quantity)` / `deleteCartItem(Long cartItemId)` — `userNo` 파라미터 부재 | `userNo` 파라미터 추가 → `cartDetailRepository.findById` 후 `cart.userNo == 호출자 userNo` 검증 → 불일치 시 `BusinessException FORBIDDEN` |
| **UserAddressService.delete 권한 분기 추가 (userNo 파라미터, JWT principal)** | 2026-04-30 | `UserAddressService.delete(long addressId)` — `userNo` 파라미터 부재로 다른 사용자 주소 삭제 가능 | `delete(long userNo, long addressId)` 시그니처 변경 → `findById` 후 `address.userNo == 호출자 userNo` 검증 → 불일치 시 `BusinessException FORBIDDEN` |
| **컨트롤러 시그니처 변경 + 프론트 영향 점검** | 2026-04-30 | `CartController` / `UserAddressController` | `@AuthenticationPrincipal UserPrincipal` 사용 강제 → 컨트롤러 메서드 시그니처 변경 가능 → 프론트(`momoolggo-fe`)의 cart/address 호출부 회귀 점검 필요 |
| **CartService UPDATE 회로 통합 테스트 추가 (권한 분기 추가와 함께, JPA dirty checking 검증)** | 2026-04-30 | `CartService.updateCartItem` — `CartServiceTest` Mockito 단위는 영속성 컨텍스트가 없어 dirty checking 실제 UPDATE 발행을 검증 못함 | Phase 2-Backfill-D — 권한 분기 추가 작업 시 `@SpringBootTest + @Transactional + @Rollback`으로 cartItem fixture INSERT → updateCartItem → flush → DB row의 quantity 갱신 확인하는 통합 테스트 1건 추가 |

> **현재 동작 동결 테스트**: `CartServiceTest`(13 케이스)와 `UserAddressServiceTest.Delete`(2 케이스)는 권한 분기 부재를 명시적으로 동결한 상태. D 단계에서 권한 분기 추가 시 해당 테스트 갱신 + 403 케이스 신규 추가 필요.

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
