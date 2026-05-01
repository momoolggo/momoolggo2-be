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

### Phase 5 또는 후속 단계

| 항목 | 발견일 | 위치 | 처리 방향 |
|---|---|---|---|
| **MapConfigController 네이버 client-id 공개 보안 부채** (`/api/map/key`) | 2026-04-29 | `MapConfigController` | 프론트 협의 후 처리 — Phase 5 |
| **OwnerStoreUpdateReq.storeId 타입 정리 (String → Long)** | 2026-04-30 | `OwnerStoreUpdateReq.java:9` `private String storeId` | D-bis에서 임시로 `Long.parseLong` 변환 + BusinessException BAD_REQUEST 처리. 근본 해결은 dto 타입 변경이지만 프론트(`StoreManagementView.vue` 등)의 storeId 전송 형식 협의 필요. |
| **OwnerService.uploadImage 확장자 화이트리스트 부재** | 2026-04-30 | `OwnerService.uploadImage` (39행) — `contentType.startsWith("image/")`만 체크 | content-type 헤더 위조 시 우회 가능. 실 확장자(jpg/png/gif/webp 등) 화이트리스트 추가 필요. multipart 10MB 제한은 적용됨. |
| **OwnerService.getOrders Feign 예외 try-catch 없음 (W-2)** | 2026-04-30 | `OwnerService.getOrders` — `authFeignClient.getUsers(userNos)` 예외 시 그대로 propagate → 점주 화면에 500 노출 | Phase 5 — `TossPaymentClient` 분리 작업과 함께 Feign Fallback 패턴 도입 (CircuitBreaker / 빈 응답 fallback). 단위 테스트 `feignException_propagates`가 현재 동작 명시 동결. |

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
| **권한 비교 패턴 비일관 (Long != long / .equals() / null 가드 혼용, 6곳)** | 2026-04-30 | 2026-05-02 | `e95cf97` `ef9a097` `aefb576` (W-A1) — Order/Cart/UserAddress 6곳 `Objects.equals()` 단일 패턴 통일 + null 가드 redundant 제거. 표준 `feedback_owner_check_pattern.md`. |
| **main-service 조회 메서드 `@Transactional(readOnly=true)` 누락 (24건, 6도메인)** | 2026-05-02 | 2026-05-02 | `38cff0b` `3db1076` `8fe7e7b` `3bf15d0` `6024407` `207996d` (B-1) — Review/Store/Order/Cart/UserAddress/Owner 일괄 적용. auth-service 패턴 일관. |
| **OwnerService 쓰기 메서드 `@Transactional` 누락 (8건, 데이터 정합성 부채)** | 2026-05-02 | 2026-05-02 | `11422c3` (B-1 확장) — `registerStore` 3 INSERT 부분 실패 위험 등 8건 일괄 처리. *발견 즉시 처리* (tech-debt 등재 X 결정). |
| **Review 통합 happy path 부재 (post/delete)** | 2026-05-02 | 2026-05-02 | `dca7c02` (B-2) — `ReviewControllerIntegrationTest`에 2건 추가. `entityManager.flush() + clear() + JPQL/findById` 재조회 검증. |
| **UserAddressService.save / setDefault 단위 테스트 부재** | 2026-05-02 | 2026-05-02 | `8ffba23` (B-3) — Save 3건(defaultAd 분기) + SetDefault 2건 추가. 누적 13건 단위 테스트. |
| **StoreController `System.out.println` 잔존 1건** | 2026-05-02 | 2026-05-02 | `ad63030` (B-4) — 디버그 잔재 단순 제거. main-service 내 `System.out` 0건 도달. |
