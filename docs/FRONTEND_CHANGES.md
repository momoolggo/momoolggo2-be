# 프론트(Vue) 변경 가이드

> MSA 전환 중 발생한 API/응답 변경사항. 학원 팀의 프론트(`momoolggo-fe`) 작업자가 참고.
> Phase 단위로 누적됨.

---

## Phase 1-B-3.5 / Phase 2-D — UserAddress 위치 정정 + AddressSearch 통합

### 1. URL 변경: UserAddress

| 기존 | 변경 |
|---|---|
| `POST /api/user/address` | `POST /api/address` |
| `GET /api/user/address` | `GET /api/address` |
| `PUT /api/user/address` | `PUT /api/address` |
| `DELETE /api/user/address/{id}` | `DELETE /api/address/{id}` |
| `PUT /api/user/address/{id}/default` | `PUT /api/address/{id}/default` |

→ axios 호출 경로 일괄 치환:
```js
// 변경 전
await axios.get('/api/user/address')

// 변경 후
await axios.get('/api/address')
```

> AddressSearch (`/api/address/search`, `/api/address/reverse`)와 MapConfig (`/api/map/key`)는 변경 없음.

### 2. 응답 변경: 회원가입 (옵션 D-1, BFF 패턴)

**기존**:
```js
const r = await axios.post('/api/user/join', { userId, userPw, ... })
// r.data === { resultMessage: '회원가입 성공', resultData: null }
// 별도로 await axios.post('/api/user/login') 호출 필요
```

**변경 후**:
```js
const r = await axios.post('/api/user/join', { userId, userPw, ... })
// r.data === {
//   resultMessage: '회원가입 성공',
//   resultData: { userNo, name, role, atExpiresAt, storeName }
// }
// HttpOnly 쿠키 access-token, refresh-token 자동 발급 → 별도 로그인 불필요
const userNo = r.data.resultData.userNo
```

**왜 변경?**: MSA 분리 후 `user`(auth) 와 `address`(main)가 다른 schema. 회원가입 시 두 schema에 동시에 INSERT하던 단일 트랜잭션이 깨졌음. 옵션 D-1 결정으로 회원가입 → 즉시 인증 토큰 발급 → 프론트가 곧장 두 번째 호출로 주소 등록하는 BFF 패턴.

### 3. 회원가입 흐름 변경 (BFF 패턴)

**기존**: `/api/user/join` 한 번 호출에 user + 기본 주소까지 같이 INSERT

**변경 후**: 두 번 호출 (순서 중요)
```js
async function register(form) {
  try {
    // 1) 회원 등록 (즉시 로그인 상태로 만듦)
    const joinRes = await axios.post('/api/user/join', {
      userId: form.userId,
      userPw: form.userPw,
      name: form.name,
      tel: form.tel,
      birth: form.birth,
      gender: form.gender,
      role: form.role
      // address 필드는 더 이상 안 보내도 됨 (auth 측에서 무시)
    })

    // 2) 주소 등록 (1번에서 발급된 토큰 자동 첨부)
    if (form.address) {
      try {
        await axios.post('/api/address', {
          address: form.address,
          addressDetail: form.addressDetail,
          latitude: form.lat,    // ⚠️ 컬럼명 변경 (lat → latitude)
          longitude: form.lng,   // ⚠️ 컬럼명 변경 (lng → longitude)
          defaultAd: 1
        })
      } catch (e) {
        // 사용자 안내: "회원가입은 완료됐지만 주소 등록은 실패했어요. 마이페이지에서 다시 등록해주세요."
        showToast('주소 등록 실패. 마이페이지에서 다시 등록해주세요.')
      }
    }

    // 회원가입 성공 처리 (토큰 이미 쿠키에 있음)
    redirectToHome()
  } catch (e) {
    // 회원가입 자체 실패 — 일반 에러 처리
    showError(e)
  }
}
```

⚠️ **원자성 약함**: 1번 성공 + 2번 실패 가능. user는 이미 만들어졌으니 로그인 가능. 마이페이지에서 주소 추가 가능. (Phase 6 Saga 패턴 검토 예정)

### 4. 컬럼명 변경: address 모델 (필드명)

| 기존 | 변경 |
|---|---|
| `lat` | `latitude` |
| `lng` | `longitude` |

→ Request body / Response 모두 적용. 프론트 `form.lat` → `form.latitude` 같이 일괄 변경.

(`address`, `addressDetail`, `defaultAd`, `addressId`, `userNo`는 변경 없음)

---

## 변경 미리 알림 (예정)

향후 Phase에서 발생할 변경:
- **Phase 4**: Gateway 도입 후 모든 API 호출이 `http://gateway:8000` 경유로 변경 (현재는 직접 main:8080, auth:8081 호출 가능)
- **Phase 4-A**: `/uploads/**` 정적 이미지 경로도 Gateway 경유
- **Phase 5**: 결제 (TOSS) 실제 키 적용 시 토스 위젯 연동 필요
