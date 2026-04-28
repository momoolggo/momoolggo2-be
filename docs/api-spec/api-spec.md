# 뭐물꼬 (momoolggo) — API 명세서

> 전체 148개 API
> 분류: 공통 9 / 고객 45 / 사장님 23 / 라이더 14 / 관리자 39 / 서버간통신 18

---

## 📌 공통 사항

### 응답 형식
모든 API는 `ResultResponse<T>` 형식 사용:
```json
{
  "resultMessage": "메시지",
  "resultData": { ... } | null | "string" | [array]
}
```

### 인증
- **AT/RT는 HttpOnly 쿠키**로 발급
- 인증 필요 API는 `인증 필요: O`로 표시
- 만료 시 RT로 자동 재발급 (`POST /api/user/reissue`)

### HTTP 상태 코드
| 코드 | 의미 |
|---|---|
| 200 OK | 성공 |
| 400 Bad Request | 입력값 오류, 비즈니스 규칙 위반 |
| 401 Unauthorized | 인증 실패, 토큰 만료 |
| 403 Forbidden | 권한 부족 |
| 404 Not Found | 리소스 없음 |
| 500 Internal Server Error | 서버 오류 |

---

## 1. 공통 (9개)

### 1.1 로그인
| 항목 | 내용 |
|---|---|
| **API ID** | user-login |
| **Method** | POST |
| **Endpoint** | `/api/user/login` |
| **인증** | X |
| **기능** | 아이디/비밀번호로 로그인 후 JWT 쿠키 발급 |

**Request Body**
```json
{
  "userId": "string",
  "userPw": "string"
}
```

**Response Body**
```json
{
  "resultMessage": "로그인 성공",
  "resultData": {
    "userNo": 1,
    "name": "string",
    "role": "CUSTOMER|OWNER|RIDER|ADMIN"
  }
}
```

**HTTP** : 200 OK / 401 Unauthorized
**비고** : AT/RT HttpOnly 쿠키 발급

---

### 1.2 로그아웃
| 항목 | 내용 |
|---|---|
| **API ID** | user-logout |
| **Method** | POST |
| **Endpoint** | `/api/user/logout` |
| **인증** | O |

**Response** : `{"resultMessage":"로그아웃 완료","resultData":null}`
**HTTP** : 200 OK

---

### 1.3 토큰 재발급
| 항목 | 내용 |
|---|---|
| **API ID** | user-reissue |
| **Method** | POST |
| **Endpoint** | `/api/user/reissue` |
| **인증** | X (RT 쿠키 필요) |
| **기능** | RT로 만료된 AT 재발급 |

**Response** : `{"resultMessage":"AT 재발급 성공","resultData":null}`
**HTTP** : 200 OK / 401 Unauthorized

---

### 1.4 아이디 중복확인
| 항목 | 내용 |
|---|---|
| **API ID** | user-check-id |
| **Method** | GET |
| **Endpoint** | `/api/user/check-id?userId={userId}` |
| **인증** | X |

**Response** : `{"resultMessage":"사용 가능","resultData":true}`
**HTTP** : 200 OK

---

### 1.5 회원가입 (고객)
| 항목 | 내용 |
|---|---|
| **API ID** | user-join |
| **Method** | POST |
| **Endpoint** | `/api/user/join` |
| **인증** | X |

**Request Body**
```json
{
  "userId": "string",
  "userPw": "string",
  "name": "string",
  "birth": "2000-01-01",
  "gender": 1,
  "tel": "010-0000-0000",
  "role": "CUSTOMER|OWNER|RIDER"
}
```

**Response Body** (Phase 1-B-3.5 변경 — 옵션 D-1 BFF 패턴)
```json
{
  "resultMessage": "회원가입 성공",
  "resultData": {
    "userNo": 18,
    "name": "string",
    "role": "CUSTOMER|OWNER|RIDER|ADMIN",
    "atExpiresAt": 1234567890000,
    "storeName": null
  }
}
```

**HTTP** : 200 OK / 400 Bad Request
**비고** :
- AT/RT HttpOnly 쿠키 자동 발급 (회원가입 직후 즉시 인증 상태 — 옵션 D-1, decisions.md 2026-04-28 참조)
- 회원가입 시 주소 정보(address, addressDetail, latitude, longitude)는 별도 `POST /api/address`로 전송 — 프론트가 두 번 호출하는 BFF 패턴 (FRONTEND_CHANGES.md 참조)
- 사장은 사업자등록 인증, 라이더는 면허 인증 필요. 관리자 승인 후 활성화.

---

### 1.6 사장 회원가입
| 항목 | 내용 |
|---|---|
| **API ID** | owner-join |
| **Method** | POST |
| **Endpoint** | `/api/owner/join` |
| **인증** | X |

**Request Body**
```json
{
  "userId": "string",
  "userPw": "string",
  "name": "string",
  "tel": "string",
  "businessNo": "string"
}
```

**Response** : `{"resultMessage":"가입 신청 완료","resultData":null}`
**HTTP** : 200 OK / 400 Bad Request
**비고** : 사업자등록번호 인증 필수

---

### 1.7 라이더 회원가입
| 항목 | 내용 |
|---|---|
| **API ID** | rider-join |
| **Method** | POST |
| **Endpoint** | `/api/rider/join` |
| **인증** | X |

**Request Body**
```json
{
  "userId": "string",
  "userPw": "string",
  "name": "string",
  "tel": "string",
  "licenseNo": "string"
}
```

**Response** : `{"resultMessage":"가입 신청 완료","resultData":null}`
**HTTP** : 200 OK / 400 Bad Request
**비고** : 면허 인증 필수

---

### 1.8 아이디 찾기
| 항목 | 내용 |
|---|---|
| **API ID** | user-find-id |
| **Method** | POST |
| **Endpoint** | `/api/user/find-id` |
| **인증** | X |

**Request Body**
```json
{
  "name": "string",
  "tel": "string"
}
```

**Response** : `{"resultMessage":"조회 성공","resultData":"user***"}`
**HTTP** : 200 OK / 404 Not Found

---

### 1.9 비밀번호 재설정
| 항목 | 내용 |
|---|---|
| **API ID** | user-reset-pw |
| **Method** | PUT |
| **Endpoint** | `/api/user/reset-pw` |
| **인증** | X |

**Request Body**
```json
{
  "userId": "string",
  "tel": "string",
  "newPw": "string"
}
```

**Response** : `{"resultMessage":"비밀번호 변경 완료","resultData":null}`
**HTTP** : 200 OK / 404 Not Found

---

## 2. 고객 (45개)

### 2.1 사용자 정보

#### 2.1.1 내 정보 조회 (간단)
| 항목 | 내용 |
|---|---|
| **API ID** | user-mypage |
| **Method** | GET |
| **Endpoint** | `/api/user/me` |
| **인증** | O |

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": {"userNo":1,"name":"string","userId":"string"}
}
```

#### 2.1.2 내 정보 상세 조회
| 항목 | 내용 |
|---|---|
| **API ID** | user-myinfo |
| **Method** | GET |
| **Endpoint** | `/api/user` |
| **인증** | O |

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": {
    "name": "string",
    "tel": "string",
    "birth": "string",
    "gender": 1,
    "greenPoint": 0
  }
}
```

#### 2.1.3 내 정보 수정
| 항목 | 내용 |
|---|---|
| **API ID** | user-myinfo-update |
| **Method** | PUT |
| **Endpoint** | `/api/user` |
| **인증** | O |

**Request Body**
```json
{
  "name": "string",
  "tel": "string",
  "userPw": "string"
}
```

**Response** : `{"resultMessage":"수정 완료","resultData":null}`

---

### 2.2 주소 관리

#### 2.2.1 주소 목록 조회
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/address` | address-list | O |

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": [{"addressId":1,"address":"string","isDefault":true}]
}
```

#### 2.2.2 주소 추가
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| POST | `/api/address` | address-add | O |

**Request Body**
```json
{
  "address": "string",
  "addressDetail": "string",
  "lat": 37.123,
  "lng": 127.123
}
```

#### 2.2.3 주소 수정
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/address/{addressId}` | address-update | O |

**Request Body**: 위와 동일
**HTTP** : 200 OK / 404 Not Found

#### 2.2.4 기본 주소 변경
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/address/{addressId}/default` | address-default | O |

#### 2.2.5 주소 삭제
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| DELETE | `/api/address/{addressId}` | address-delete | O |

#### 2.2.6 주소 검색 (네이버 API)
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/address/search?query={검색어}` | address-search | O |

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": [{"address":"string","lat":37.123,"lng":127.123}]
}
```

#### 2.2.7 좌표→주소 변환 (Reverse Geocoding)
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/address/reverse?lat={}&lng={}` | address-reverse | O |

#### 2.2.8 지도 API 키 조회
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/map/key` | map-key | O |

**Response** : `{"resultMessage":"조회 성공","resultData":"CLIENT_ID"}`

---

### 2.3 가게 조회

#### 2.3.1 가게 목록
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/store?category=&sort=&keyword=` | user-store-list | O |

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": [{"storeId":1,"storeName":"string","rating":4.5}]
}
```

#### 2.3.2 가게 상세
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/store/{storeId}` | user-store-detail | O |

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": {
    "storeId": 1,
    "storeName": "string",
    "menus": [],
    "reviews": []
  }
}
```

#### 2.3.3 가게/메뉴 검색
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/store/search?keyword={}` | user-store-search | O |

#### 2.3.4 가게 내 메뉴 검색
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/store/{storeId}/menu/search?keyword={}` | user-store-menu-search | O |

#### 2.3.5 주변 가게 (반경 3km)
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/store/nearby?lat=&lng=&radius=` | user-nearby | O |

---

### 2.4 찜

#### 2.4.1 찜 목록
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/user/favorite` | user-favorite-list | O |

#### 2.4.2 찜 등록/해제 (토글)
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| POST | `/api/user/favorite/{storeId}` | user-favorite-toggle | O |

**Response**
```json
{
  "resultMessage": "찜 등록/해제 완료",
  "resultData": {"isFavorite": true}
}
```

---

### 2.5 장바구니

#### 2.5.1 장바구니 조회
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/cart` | user-cart-list | O |

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": {
    "items": [],
    "totalPrice": 0,
    "deliveryTip": 0
  }
}
```

#### 2.5.2 장바구니 담기
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| POST | `/api/cart` | user-cart-add | O |

**Request Body**
```json
{
  "menuId": 1,
  "quantity": 2,
  "options": [1, 2]
}
```

**비고** : 다른 가게 메뉴 담기 시 경고

#### 2.5.3 수량 변경
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/cart/{cartItemId}` | user-cart-update | O |

**Request Body** : `{"quantity": 3}`

#### 2.5.4 항목 삭제
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| DELETE | `/api/cart/{cartItemId}` | user-cart-delete | O |

#### 2.5.5 전체 비우기
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| DELETE | `/api/cart` | user-cart-clear | O |

---

### 2.6 주문 / 결제

#### 2.6.1 주문 생성
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| POST | `/api/order` | user-order-create | O |

**Request Body**
```json
{
  "storeId": 1,
  "items": [{"menuId":1,"quantity":2,"options":[1]}],
  "addressId": 1,
  "request": "string",
  "couponId": null,
  "greenPoint": 0
}
```

**Response**
```json
{
  "resultMessage": "주문 생성",
  "resultData": {"orderId":1,"totalAmount":15000}
}
```

**비고** : 친환경 선택 시 친환경 점수 적립

#### 2.6.2 결제 승인 (토스페이먼츠)
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| POST | `/api/payment/confirm` | payment-confirm | O |

**Request Body**
```json
{
  "paymentKey": "string",
  "orderId": "string",
  "amount": 15000
}
```

#### 2.6.3 주문 취소
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/order/{orderId}/cancel` | user-cancel | O |

**Request Body** : `{"reason": "단순 변심"}`
**비고** : 가게 승인 후 취소 불가

#### 2.6.4 결제 취소(환불)
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| POST | `/api/payment/{orderId}/refund` | user-refund | O |

#### 2.6.5 주문 내역 조회
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/order/history?userNo=&page=` | user-orderlist | O |

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": {"orders":[],"maxPage":5}
}
```

#### 2.6.6 주문 상세
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/order/history/{orderId}` | user-order-detail | O |

#### 2.6.7 배달 현황 (실시간)
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/order/{orderId}/status` | user-order-status | O |

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": {"status":"COOKING","riderLat":null}
}
```

**비고** : SSE 연동, 실시간 갱신

---

### 2.7 쿠폰

#### 2.7.1 쿠폰함 조회
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/user/coupon` | user-coupon-list | O |

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": [{
    "couponId": 1,
    "name": "string",
    "discount": 1000,
    "expiry": "2025-12-31"
  }]
}
```

#### 2.7.2 쿠폰 적용
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| POST | `/api/user/coupon/apply` | user-coupon-apply | O |

**Request Body** : `{"orderId":1,"couponId":1}`

---

### 2.8 리뷰

#### 2.8.1 리뷰 작성
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| POST | `/api/review` | user-review-post | O |

**Request Body**
```json
{
  "orderId": 1,
  "storeId": 1,
  "rating": 5,
  "content": "string",
  "images": ["base64..."]
}
```

#### 2.8.2 내 리뷰 목록
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/user/review?page=` | user-review-list | O |

#### 2.8.3 리뷰 수정
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/review/{reviewId}` | user-review-update | O |

**Request Body** : `{"rating":4,"content":"수정 내용"}`

#### 2.8.4 리뷰 삭제
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| DELETE | `/api/review/{reviewId}` | user-review-delete | O |

---

### 2.9 이벤트 / 펫 / 챗봇

#### 2.9.1 룰렛 이벤트
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| POST | `/api/event/roulette` | user-event-roulette | O |

**Response**
```json
{
  "resultMessage": "참여 완료",
  "resultData": {"result":"치킨","couponId":5}
}
```

**비고** : 1일 1회 무료

#### 2.9.2 미니게임
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| POST | `/api/event/game` | user-event-game | O |

**Request Body** : `{"gameType":"cup_shuffle","result":"WIN"}`

#### 2.9.3 펫 정보 조회
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/user/pet` | user-pet-info | O |

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": {
    "level": 3,
    "exp": 450,
    "nextLevelExp": 500,
    "cosmetics": []
  }
}
```

#### 2.9.4 펫 먹이 주기
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| POST | `/api/user/pet/feed` | user-pet-feed | O |

**Response**
```json
{
  "resultMessage": "먹이 주기 완료",
  "resultData": {"exp":460,"levelUp":false}
}
```

#### 2.9.5 멤버십 정보 조회
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/user/membership` | user-membership | O |

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": {
    "grade": "SILVER",
    "greenPoint": 120,
    "petLevel": 3
  }
}
```

#### 2.9.6 1:1 문의 챗봇
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| POST | `/api/chatbot/message` | user-chatbot | O |

**Request Body** : `{"message":"배달이 안 왔어요"}`
**Response**
```json
{
  "resultMessage": "응답 완료",
  "resultData": {
    "reply": "string",
    "suggestions": [],
    "adminContactUrl": "string or null"
  }
}
```

**비고** : Gemini API 연동, 챗봇 미해결 시 adminContactUrl 반환

---

### 2.10 정책 / 고객센터

#### 2.10.1 정책 열람
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/policy/{type}` | user-policy-view | X |

**Path Parameter** : `type` = cancel | refund | privacy

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": {
    "title": "string",
    "content": "string",
    "version": 2
  }
}
```

#### 2.10.2 정책 동의
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| POST | `/api/policy/agree` | user-policy-agree | X |

**Request Body** : `{"policyIds":[1,2,3]}`
**비고** : 필수 정책 미동의 시 가입 불가

#### 2.10.3 FAQ 조회
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/cs/faq` | user-cs-faq | O |

---

## 3. 사장님 (23개)

### 3.1 가게 관리

#### 3.1.1 가게 등록 여부 확인
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/owner/store/check` | owner-store-check | O |

#### 3.1.2 내 가게 목록
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/owner/store` | owner-store-list | O |

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": [{"storeId":1,"storeName":"string"}]
}
```

#### 3.1.3 가게 등록
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| POST | `/api/owner/store` | owner-store-add | O |

**Request Body**
```json
{
  "storeName": "string",
  "address": "string",
  "tel": "string",
  "businessNo": "string",
  "category": "string",
  "minOrderPrice": 12000
}
```

#### 3.1.4 가게 정보 수정
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/owner/store` | owner-store-update | O |

**Request Body**
```json
{
  "storeId": 1,
  "storeName": "string",
  "address": "string",
  "tel": "string",
  "photo": "base64..."
}
```

#### 3.1.5 가게 운영 상태 수정
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/owner/store/status` | owner-store-status | O |

**Request Body**
```json
{
  "storeId": 1,
  "isOpen": true,
  "openTime": "09:00",
  "closeTime": "22:00",
  "closedDays": [0],
  "minOrderPrice": 15000,
  "notice": "string"
}
```

#### 3.1.6 가게 삭제
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| DELETE | `/api/owner/store/{storeId}` | owner-store-delete | O |

---

### 3.2 메뉴 관리

#### 3.2.1 메뉴 목록
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/owner/menu?storeId=` | owner-menu-list | O |

#### 3.2.2 메뉴 추가
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| POST | `/api/owner/menu` | owner-menu-add | O |

**Request Body**
```json
{
  "storeId": 1,
  "categoryId": 1,
  "menuName": "string",
  "price": 10000,
  "description": "string",
  "image": "base64..."
}
```

#### 3.2.3 메뉴 수정
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/owner/menu` | owner-menu-update | O |

**Request Body**
```json
{
  "menuId": 1,
  "menuName": "string",
  "price": 12000,
  "description": "string"
}
```

#### 3.2.4 메뉴 삭제
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| DELETE | `/api/owner/menu/{menuId}` | owner-menu-delete | O |

---

### 3.3 메뉴 옵션

#### 3.3.1 옵션 추가
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| POST | `/api/owner/menu/{menuId}/option` | owner-menu-option-add | O |

**Request Body**
```json
{
  "optionGroup": "맵기 선택",
  "options": [
    {"name":"순한맛","price":0},
    {"name":"매운맛","price":500}
  ]
}
```

#### 3.3.2 옵션 수정
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/owner/menu/option/{optionId}` | owner-menu-option-update | O |

#### 3.3.3 옵션 삭제
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| DELETE | `/api/owner/menu/option/{optionId}` | owner-menu-option-delete | O |

---

### 3.4 카테고리 관리

#### 3.4.1 카테고리 목록
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/owner/category?storeId=` | owner-category-list | O |

#### 3.4.2 카테고리 추가
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| POST | `/api/owner/category` | owner-category-add | O |

**Request Body** : `{"storeId":1,"categoryName":"사이드"}`

#### 3.4.3 카테고리 수정
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/owner/category/{categoryId}` | owner-category-update | O |

#### 3.4.4 카테고리 삭제
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| DELETE | `/api/owner/category/{categoryId}` | owner-category-delete | O |

**비고** : 포함 메뉴 함께 삭제, 삭제 전 경고 표시

---

### 3.5 주문 관리

#### 3.5.1 주문 현황 조회
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/owner/order?storeId=&status=` | owner-orderlist | O |

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": {
    "total": 10,
    "waiting": 3,
    "cooking": 2,
    "delivering": 4,
    "completed": 1
  }
}
```

#### 3.5.2 주문 상태 변경
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/owner/order/{orderId}` | owner-order-status | O |

**Request Body** : `{"status":"ACCEPTED"}`

#### 3.5.3 실시간 주문 알림 (SSE)
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/owner/order/subscribe?storeId=` | owner-order-alert | O |

**Response (SSE Stream)**
```
data: {"orderId":1,"type":"NEW_ORDER"}
```

---

### 3.6 리뷰 / 매출

#### 3.6.1 고객 리뷰 조회
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/owner/review?storeId=&page=` | owner-review-list | O |

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": {"avgRating":4.2,"reviews":[]}
}
```

#### 3.6.2 리뷰 신고
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| POST | `/api/owner/review/{reviewId}/report` | owner-review-report | O |

**Request Body** : `{"reason":"악성 리뷰"}`

#### 3.6.3 매출 현황 조회
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/owner/sales?storeId=&startDate=&endDate=` | owner-sales | O |

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": {
    "totalSales": 500000,
    "dailyData": [],
    "menuRanking": []
  }
}
```

---

## 4. 라이더 (14개)

### 4.1 배달 처리

#### 4.1.1 대기 배달 목록
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/rider/order/waiting` | rider-order-waiting | O |

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": [{
    "orderId": 1,
    "storeName": "string",
    "address": "string",
    "distance": 2.3
  }]
}
```

#### 4.1.2 배달 수락
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/rider/order/{orderId}/accept` | rider-order-accept | O |

#### 4.1.3 배달 반려
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/rider/order/{orderId}/reject` | rider-order-reject | O |

#### 4.1.4 진행 중 배달 조회
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/rider/order/inprogress` | rider-order-inprogress | O |

#### 4.1.5 가게 도착
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/rider/order/{orderId}/arrive` | rider-order-arrive | O |

#### 4.1.6 배달 완료
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/rider/order/{orderId}/complete` | rider-order-complete | O |

**Request Body** : `{"photo":"base64..."}`
**비고** : 카메라 촬영→업로드→배달 완료, 고객에게 알림

---

### 4.2 정산 / 내역

#### 4.2.1 배달 내역 조회
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/rider/order/history?startDate=&endDate=` | rider-order-history | O |

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": {
    "totalDeliveries": 45,
    "totalDistance": 120.5,
    "totalFee": 135000,
    "weeklySettlement": {
      "deliveryFee": 50000,
      "fee": 5000,
      "netAmount": 45000
    }
  }
}
```

#### 4.2.2 정산 계좌 조회
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/rider/settlement/account` | rider-settlement-account | O |

#### 4.2.3 정산 계좌 변경
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/rider/settlement/account` | rider-settlement-account-update | O |

**Request Body**
```json
{
  "bankName": "string",
  "accountNo": "string",
  "holder": "string"
}
```

---

### 4.3 상태 / 위치 / 기타

#### 4.3.1 라이더 상태 변경
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/rider/status` | rider-status-toggle | O |

**Request Body** : `{"status":"ACTIVE"}`
**비고** : ACTIVE(배달중) / REST(휴식중)

#### 4.3.2 업무 종료
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| POST | `/api/rider/off` | rider-off | O |

**비고** : 진행 중 배달 있으면 종료 불가

#### 4.3.3 공지사항 조회
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/rider/notice` | rider-notice-list | O |

#### 4.3.4 위치 정보 갱신
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/rider/location` | rider-location-update | O |

**Request Body** : `{"lat":35.123,"lng":128.456}`
**비고** : 실시간 위치 추적 (주기적 호출)

#### 4.3.5 고객센터 연결
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/rider/cs/contact` | rider-cs-call | O |

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": {
    "tel": "1588-0000",
    "availableTime": "09:00~18:00"
  }
}
```

---

## 5. 관리자 (39개)

### 5.1 대시보드 / 회원 관리

#### 5.1.1 대시보드
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/admin/dashboard` | admin-dashboard | O (ADMIN) |

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": {
    "totalUsers": 500,
    "totalStores": 120,
    "totalReviews": 3400
  }
}
```

#### 5.1.2 회원 목록
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/admin/user?keyword=&role=&status=&page=` | admin-user-list | O (ADMIN) |

#### 5.1.3 회원 가입 승인
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/admin/user/{userNo}/approve` | admin-user-approve | O (ADMIN) |

#### 5.1.4 회원 페널티 부여
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/admin/user/{userNo}/penalty` | admin-user-penalty | O (ADMIN) |

**Request Body**
```json
{
  "penaltyType": "WARNING",
  "reason": "string"
}
```

**비고** : WARNING (경고 3회) / RESTRICT (일시제한 5회) / SUSPEND (정지 7회 이상)

#### 5.1.5 회원 탈퇴
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| DELETE | `/api/admin/user/{userNo}` | admin-user-delete | O (ADMIN) |

---

### 5.2 가게 관리

#### 5.2.1 가게 목록
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/admin/store?keyword=&status=&page=` | admin-store-list | O (ADMIN) |

#### 5.2.2 가게 영업 승인/보류/반려
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/admin/store/{storeId}/approve` | admin-store-approve | O (ADMIN) |

**Request Body** : `{"status":"APPROVED"}`

---

### 5.3 리뷰 관리

#### 5.3.1 리뷰 목록
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/admin/review?type=&page=` | admin-review-list | O (ADMIN) |

**비고** : type = 전체/신고

#### 5.3.2 신고 리뷰 처리
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| PUT | `/api/admin/review/{reviewId}/action` | admin-review-action | O (ADMIN) |

**Request Body**
```json
{
  "action": "RESTRICT",
  "restrictType": "WRITE_BAN"
}
```

---

### 5.4 공지사항 (라이더 대상)

| API ID | Method | Endpoint |
|---|---|---|
| admin-notice-list | GET | `/api/admin/notice?page=` |
| admin-notice-create | POST | `/api/admin/notice` |
| admin-notice-update | PUT | `/api/admin/notice/{noticeId}` |
| admin-notice-delete | DELETE | `/api/admin/notice/{noticeId}` |

**등록 Request Body**
```json
{
  "title": "string",
  "content": "string",
  "category": "SAFETY"
}
```

---

### 5.5 정책 관리

| API ID | Method | Endpoint |
|---|---|---|
| admin-policy-list | GET | `/api/admin/policy?type=&isActive=` |
| admin-policy-create | POST | `/api/admin/policy` |
| admin-policy-update | PUT | `/api/admin/policy/{policyId}` |
| admin-policy-deactivate | PUT | `/api/admin/policy/{policyId}/deactivate` |
| admin-policy-greenpoint | GET | `/api/admin/policy/greenpoint` |

**등록 Request Body**
```json
{
  "type": "cancel",
  "title": "string",
  "content": "string"
}
```

**비고**
- type = cancel | refund | privacy
- 수정 시 기존 버전 비활성화 → 새 버전 (version+1)
- 비활성화 = 논리 삭제

---

### 5.6 라이더 관제

#### 5.6.1 라이더 배달 관제
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/admin/rider?riderNo=&status=` | admin-rider-list | O (ADMIN) |

**Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": [{
    "riderNo": 1,
    "name": "string",
    "status": "ACTIVE",
    "lat": 35.1,
    "lng": 128.4,
    "currentOrderId": null
  }]
}
```

#### 5.6.2 배달 상세
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| GET | `/api/admin/rider/delivery/{deliveryId}` | admin-rider-delivery-detail | O (ADMIN) |

#### 5.6.3 라이더 연락 처리
| Method | Endpoint | API ID | 인증 |
|---|---|---|---|
| POST | `/api/admin/rider/{riderNo}/contact` | admin-rider-contact | O (ADMIN) |

**Request Body**
```json
{
  "contactType": "CALL",
  "message": "string"
}
```

#### 5.6.4 라이더 전체 공지
| API ID | Method | Endpoint |
|---|---|---|
| admin-rider-notice-list | GET | `/api/admin/rider/notice?page=&sendType=` |
| admin-rider-notice-send | POST | `/api/admin/rider/notice` |
| admin-rider-notice-update | PUT | `/api/admin/rider/notice/{noticeId}` |
| admin-rider-notice-delete | DELETE | `/api/admin/rider/notice/{noticeId}` |

**발송 Request Body**
```json
{
  "title": "string",
  "targetType": "ALL",
  "content": "string",
  "sendType": "NOW",
  "reservedAt": null
}
```

**비고** : sendType = NOW (즉시) / RESERVE (예약)

---

### 5.7 정산 관리

| API ID | Method | Endpoint |
|---|---|---|
| admin-settlement-summary | GET | `/api/admin/settlement/summary?startDate=&endDate=&targetType=` |
| admin-settlement-list | GET | `/api/admin/settlement?targetType=&targetId=&status=&...` |
| admin-settlement-detail | GET | `/api/admin/settlement/{settlementId}` |
| admin-settlement-order-list | GET | `/api/admin/settlement/{settlementId}/orders?week=` |
| admin-settlement-filter-summary | GET | `/api/admin/settlement/filter-summary?...` |
| admin-settlement-target-detail | GET | `/api/admin/settlement/target/{targetType}/{targetId}` |

**비고**
- targetType = STORE | RIDER | ALL
- 정산은 주 단위 기준으로 집계

---

### 5.8 고객 지원

| API ID | Method | Endpoint |
|---|---|---|
| admin-cs-summary | GET | `/api/admin/cs/summary` |
| admin-cs-inquiry-list | GET | `/api/admin/cs/inquiry?userType=&status=&page=` |
| admin-cs-inquiry-detail | GET | `/api/admin/cs/inquiry/{inquiryId}` |
| admin-cs-inquiry-reply | POST | `/api/admin/cs/inquiry/{inquiryId}/reply` |

**문의 현황 카드 Response**
```json
{
  "resultMessage": "조회 성공",
  "resultData": {
    "totalInquiryCount": 0,
    "autoResolvedCount": 0,
    "pendingInquiryCount": 0
  }
}
```

#### FAQ 관리
| API ID | Method | Endpoint |
|---|---|---|
| admin-cs-faq-list | GET | `/api/admin/cs/faq?category=&page=` |
| admin-cs-faq-create | POST | `/api/admin/cs/faq` |
| admin-cs-faq-update | PUT | `/api/admin/cs/faq/{faqId}` |
| admin-cs-faq-delete | DELETE | `/api/admin/cs/faq/{faqId}` |

**FAQ 등록 Request Body**
```json
{
  "question": "string",
  "answer": "string",
  "category": "결제"
}
```

**비고** : category = 결제 / 배달 / 환불취소 / 회원

---

## 6. 서버 간 통신 (Internal API, 18개)

> 외부 노출 X. MSA 서비스 간 호출용 (FeignClient).
> Gateway를 거치지 않고 서비스 간 직접 통신.

### 6.1 Gateway → Auth

#### 6.1.1 토큰 검증
| Method | Endpoint | API ID |
|---|---|---|
| POST | `/internal/auth/validate` | internal-token-validate |

**Request** : `{"accessToken": "string"}`
**Response** : `{"valid":true,"userNo":1,"role":"CUSTOMER"}`
**비고** : 모든 인증 요청 선행

#### 6.1.2 유저 역할 조회
| Method | Endpoint | API ID |
|---|---|---|
| GET | `/internal/auth/user/{userNo}/role` | internal-user-role |

**Response** : `{"userNo":1,"role":"OWNER","status":"ACTIVE"}`

---

### 6.2 Main → Auth

#### 6.2.1 유저 정보 조회 (배송용)
| Method | Endpoint | API ID |
|---|---|---|
| GET | `/internal/auth/user/{userNo}` | internal-user-info |

**Response** : `{"userNo":1,"name":"string","tel":"string","address":"string"}`

#### 6.2.2 사장 정보 조회
| Method | Endpoint | API ID |
|---|---|---|
| GET | `/internal/auth/owner/{userNo}` | internal-owner-info |

---

### 6.3 Main → Rider

#### 6.3.1 라이더 배차 요청
| Method | Endpoint | API ID |
|---|---|---|
| POST | `/internal/rider/assign` | internal-rider-assign |

**Request Body**
```json
{
  "orderId": 1,
  "storeId": 1,
  "storeLat": 35.12,
  "storeLng": 128.45,
  "deliveryAddress": "string",
  "deliveryLat": 35.13,
  "deliveryLng": 128.46
}
```

**Response** : `{"assigned":true,"riderId":5}`

#### 6.3.2 라이더 위치 조회
| Method | Endpoint | API ID |
|---|---|---|
| GET | `/internal/rider/{riderNo}/location` | internal-rider-location |

**Response**
```json
{
  "riderNo": 5,
  "lat": 35.125,
  "lng": 128.458,
  "updatedAt": "2025-06-01T12:30:00"
}
```

#### 6.3.3 라이더 상태 확인
| Method | Endpoint | API ID |
|---|---|---|
| GET | `/internal/rider/{riderNo}/status` | internal-rider-status-check |

---

### 6.4 Rider → Main

#### 6.4.1 배달 상태 변경 알림
| Method | Endpoint | API ID |
|---|---|---|
| PUT | `/internal/order/{orderId}/delivery-status` | internal-order-status-update |

**Request Body** : `{"status":"PICKED_UP","riderNo":5}`
**비고** : SSE로 고객/사장에게 전달

#### 6.4.2 배달 완료 처리
| Method | Endpoint | API ID |
|---|---|---|
| POST | `/internal/order/{orderId}/complete` | internal-delivery-complete |

**Request Body**
```json
{
  "riderNo": 5,
  "photo": "base64...",
  "completedAt": "2025-06-01T13:00:00"
}
```

**비고** : 펫 성장치/그린포인트 적립 트리거

---

### 6.5 Admin → Auth

#### 6.5.1 회원 승인 처리
| Method | Endpoint | API ID |
|---|---|---|
| PUT | `/internal/auth/user/{userNo}/approve` | internal-user-approve |

**Request Body** : `{"status":"APPROVED"}`

#### 6.5.2 회원 차단 처리
| Method | Endpoint | API ID |
|---|---|---|
| PUT | `/internal/auth/user/{userNo}/block` | internal-user-block |

**Request Body** : `{"status":"BLOCKED","reason":"string"}`

---

### 6.6 Admin → Main

#### 6.6.1 가게 영업 승인
| Method | Endpoint | API ID |
|---|---|---|
| PUT | `/internal/store/{storeId}/approve` | internal-store-approve |

#### 6.6.2 리뷰 활동 제한
| Method | Endpoint | API ID |
|---|---|---|
| PUT | `/internal/review/{reviewId}/restrict` | internal-review-restrict |

**Request Body**
```json
{
  "restrictType": "WRITE_BAN",
  "targetUserNo": 10
}
```

#### 6.6.3 그린포인트 적립
| Method | Endpoint | API ID |
|---|---|---|
| POST | `/internal/user/{userNo}/greenpoint` | internal-greenpoint-add |

**Request Body** : `{"point":10,"reason":"일회용품 미사용"}`
**비고** : 배달 완료 시 자동 트리거

#### 6.6.4 정산 데이터 조회
| Method | Endpoint | API ID |
|---|---|---|
| GET | `/internal/settlement?startDate=&endDate=&targetType=&status=&...` | internal-admin-settlement-data |

#### 6.6.5 고객지원 데이터 조회
| Method | Endpoint | API ID |
|---|---|---|
| GET | `/internal/cs?userType=&status=&inquiryId=&page=` | internal-admin-cs-data |

---

### 6.7 Admin → Rider

#### 6.7.1 라이더 관제 데이터
| Method | Endpoint | API ID |
|---|---|---|
| GET | `/internal/rider/monitor?status=&page=` | internal-admin-rider-monitor |

**Response**
```json
{
  "summary": {
    "waiting": 0,
    "assigned": 0,
    "delivering": 0,
    "completed": 0
  },
  "deliveries": []
}
```

#### 6.7.2 라이더 전체 공지 발송
| Method | Endpoint | API ID |
|---|---|---|
| POST | `/internal/rider/notice` | internal-admin-rider-notice-send |

---

## 📊 통계 요약

| 카테고리 | API 수 |
|---|---|
| 공통 | 9 |
| 고객 | 45 |
| 사장님 | 23 |
| 라이더 | 14 |
| 관리자 | 39 |
| 서버 간 통신 | 18 |
| **합계** | **148** |

## 🗂 서비스별 매핑 (MSA Gateway 라우팅)

| 경로 패턴 | 라우팅 대상 |
|---|---|
| `/api/user/**`, `/api/owner/join`, `/api/rider/join`, `/api/policy/**` | **Auth Service (8081)** |
| `/api/store/**`, `/api/cart/**`, `/api/order/**`, `/api/payment/**`, `/api/owner/**` (join 제외), `/api/review/**`, `/api/user/favorite/**`, `/api/user/coupon/**`, `/api/user/pet/**`, `/api/user/membership`, `/api/event/**`, `/api/chatbot/**`, `/api/cs/**`, `/api/address/**`, `/api/map/**`, `/uploads/**` | **Main Service (8080)** |
| `/api/rider/**` (join 제외) | **Rider Service (8082)** |
| `/api/admin/**` | **Admin Service (8083)** |
| `/internal/**` | 외부 노출 X (Gateway 차단) |
