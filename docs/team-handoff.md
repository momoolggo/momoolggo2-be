# 팀원 영역 핸드오프

본인이 발견한 부채 중 **팀원 영역 모듈** 작업이 필요한 항목 박제. 본인 처리 X (CLAUDE.md §6 #12).

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
| **영역** | `mmg-gateway` ❓ 영역 미상 (확인 필요) |
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
| **영역** | `mmg-gateway` ❓ 영역 미상 (확인 필요) |
| **위치** | `mmg-gateway/.../GatewayIntegrationTest.java:99-111` |
| **경위** | Phase 4-B reviewer Warning 잔존 (cosmetic) |
| **권장 처리** | lambda → `status().is(not(403))` 또는 AssertJ |
| **참조 부채** | `docs/tech-debt.md` "GatewayIntegrationTest cosmetic 2건" 항목 |

### 5. Phase 4-B Warning 2 — `GatewayIntegrationTest` 클래스 Javadoc 잔존 주석

| 항목 | 내용 |
|---|---|
| **영역** | `mmg-gateway` ❓ 영역 미상 (확인 필요) |
| **위치** | `mmg-gateway/.../GatewayIntegrationTest.java:40-41` |
| **경위** | Phase 4-B reviewer Warning 잔존 (cosmetic) |
| **현재 상태** | `"Step 4·5 추가 예정"` 잔존 주석 (Step 4·5 이미 구현됨) |
| **권장 처리** | 주석 제거 또는 `"구현됨"` 정정 |
| **참조 부채** | `docs/tech-debt.md` "GatewayIntegrationTest cosmetic 2건" 항목 |

---

## 처리 완료

(팀원 처리 완료 시 이력 박제 — 항목 / 처리 커밋 / 처리일)
