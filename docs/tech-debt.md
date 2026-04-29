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

### Phase 2-Backfill-C 예정

| 항목 | 발견일 | 위치 | 처리 시점 |
|---|---|---|---|
| **ReviewService 예외 케이스 (403 주문자 불일치 / 409 중복 리뷰) 테스트 0** | 2026-04-29 | `ReviewService.postReview` | Phase 2-Backfill-C |
| **CartService.clearAndAddToCart / deleteCartItem 단위 테스트 0** | 2026-04-29 | `CartService` | Phase 2-Backfill-C |
| **UserAddressService.delete addressId 유효성 검증 없음** | 2026-04-29 | `UserAddressService.delete` | Phase 2-Backfill-C |

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
