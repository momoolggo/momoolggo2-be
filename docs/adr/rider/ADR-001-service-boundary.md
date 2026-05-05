# ADR-001: 라이더 서비스 경계

> **상태**: Accepted (2026-05-05) — D11 임시 운영 추가 (2026-05-05)
> **관련 결정**: Q1 (가입 위치), Q2 (면허/승인 흐름), D11 (admin 의존성 임시 우회)
> **관련 Figma**: `../../figma/` 회원가입/로그인 화면 (figma-analysis.md ADR-001 매핑)

---

## 상황 (Context)

라이더 도메인은 CLAUDE.md §3에서 별도 마이크로서비스 `mmg-rider-service`로 명시 (포트 8082). 현재 상태:
- mmg-rider-service: HelloController + RiderApplication만 (사실상 빈 모듈)
- 의존성: mmg-common + spring-boot-starter-web (Phase 0-B 그대로)
- DB 스키마: 미생성 (mmg_rider 신설 필요)
- 인증/권한 인프라: mmg-common JwtUser.role 문자열에 RIDER 정의 (enum 부재 — 진단 정정 #6)

라이더 가입/로그인을 어디서 처리할지가 첫 갈림길. 인증 단일 소스 vs 도메인 단일 트랜잭션의 트레이드오프.

---

## 옵션 (Options)

### A. auth-service에 가입 endpoint, license/account를 같은 트랜잭션에서 rider Feign으로 생성

- 장점: 한 번의 가입 요청으로 user + rider 프로필 동시 생성, UX 단순
- 단점: **사가 패턴 위험** — auth user 생성 성공 후 rider 프로필 Feign 실패 시 보상 트랜잭션 필요. Outbox/Saga 인프라가 Phase 6+에 도입 예정이라 현 시점에 도입은 과설계

### B. rider-service에 가입 endpoint, 내부에서 auth Feign으로 user 생성

- 장점: rider 도메인 단일 트랜잭션에서 처리, rider-service가 라이더 가입 로직 소유
- 단점: 동일한 사가 패턴 위험. 또한 auth-service의 회원가입 로직(검증/암호화) 중복 또는 분기 발생

### C. auth-service에 가입 endpoint만 두고, license/account는 가입 후 별도 endpoint (`PUT /api/rider/profile`)

- 장점: 사가 패턴 회피 (각 endpoint 단일 트랜잭션). 4-A InternalUserController + JpaRepository 패턴 그대로 재사용. 인증 단일 소스
- 단점: 가입 UX가 2단계 (회원가입 → 프로필 작성)

---

## 결정 (Decision)

**(C) 채택** — auth-service에 회원가입 endpoint, 라이더 추가정보(license_type, vehicle_type, account 등)는 가입 직후 rider-service의 별도 endpoint로 등록.

### 흐름

```
1. POST /api/rider/join (auth-service)
   - userId, password, name, tel, role=RIDER
   - 기존 USER 가입 흐름 그대로 (UserService.signup 분기)
   - 응답: AT/RT 쿠키 + user_no
2. PUT /api/rider/profile (rider-service, 인증 필요)
   - license_no, license_type, vehicle_type, account_bank, account_no, account_holder
   - 신규 rider entity INSERT (status=PENDING)
   - 응답: rider_no
3. (admin 단계, ADR-009 별도) admin이 PENDING 라이더 승인 → status=ACTIVE
```

---

## 결과 (Consequences)

### 인증 흐름

- JWT 발급은 auth-service 단일 책임 유지. role=RIDER로 분기.
- mmg-common JwtUser.role 문자열 그대로 활용 (enum 도입은 별건, 본 ADR 범위 외)
- RefreshTokenStore 그대로 auth-service에 잔존 (메모리 tech-debt "이관" 항목 폐기)

### Feign 인터페이스

- AuthInternalClient (Phase 4-A `InternalUserController`) 그대로 — 라이더 권한 정보 조회 시 RIDER role 필터링 추가 검토
- rider-service → auth-service 직접 Feign 호출 없음 (가입은 클라이언트가 두 endpoint 순차 호출)

### 권한 검증

- TokenAuthenticationFilter (mmg-common) RIDER role 인식 → SecurityContextHolder에 Authentication 설정
- rider-service의 BaseSecurityConfig는 `/api/rider/**` 모두 ROLE_RIDER 필요 (단, /join은 auth-service이므로 무관)

### 학원 발표 데모

- 데모 1 (figma-analysis.md): 회원가입 → admin 승인 → ACTIVE
- 사전 데이터: ACTIVE 라이더 1명 fixture INSERT (시연 시 PENDING 흐름은 별도 시연)

---

## 트레이드오프

| 항목 | (C) 채택 결과 | 미래 고려 사항 |
|---|---|---|
| 사가 패턴 | 회피 (단일 endpoint 단일 트랜잭션) | Phase 6+ 라이더 가입 시 license 인증 외부 API 통합 시 Outbox 도입 검토 |
| UX | 2단계 가입 (회원가입 → 프로필) | 프론트가 자동으로 두 endpoint 순차 호출하면 사용자 체감 1단계 |
| 인증 단일성 | 보장 (auth-service만 JWT 발급) | RIDER role 분기가 늘어날 시 auth-service 비대화 위험 — Phase 6+ 분리 검토 |
| 코드 재사용 | 4-A InternalUserController 패턴 그대로 | Phase 5-R1에서 검증 |

---

## 미해결 / Phase 5에서 결정

- license_type 검증 (외부 API 연동) → 학원 발표 시 mocking
- 가입 시 약관 동의 흐름 → auth-service의 기존 정책 동의 패턴 활용 (Phase 1 폴리시 도메인)

---

## 임시 운영 (admin-service 도입 전) — D11

Q2-B 결정 (PENDING → admin 승인 → ACTIVE)은 admin-service의 승인 endpoint
(`POST /internal/rider/{riderNo}/approve`, interfaces.md §3.1)에 의존한다.

**제약**: admin-service는 다른 팀원 작업 영역으로 현재 상태 미상. 충돌 회피 위해
rider-service에서는 admin-service에 어떤 endpoint도 추가/수정/삭제하지 않는다.

### 임시 처리 (D11 옵션 A-1: profile toggle)

`rider-service` `application.yml`에 `rider.auto-approve` toggle 추가:
- 개발 / 학원 발표: `true` → 가입 직후 자동 ACTIVE 전환
- 운영 (admin 도입 후): `false` → PENDING 정상 흐름, admin approve 대기

`RiderService.join()` 내 명시적 블록 + TODO 주석 (Phase 5-R1에서 구현):

```java
public void join(RiderProfileRequest req, long callerUserNo) {
    Rider rider = new Rider(callerUserNo, req);
    rider.setStatus(RiderStatus.PENDING);
    rider = riderRepository.save(rider);

    // === 임시: admin-service 미도입 시 자동 ACTIVE (D11 옵션 A-1) ===
    // TODO: admin-service approve endpoint 도입 후 이 블록 제거
    //       + application.yml `rider.auto-approve: false`
    if (riderProperties.isAutoApprove()) {
        rider.setStatus(RiderStatus.ACTIVE);
        riderRepository.save(rider);
    }
}
```

```yaml
# rider-service application.yml
rider:
  auto-approve: ${RIDER_AUTO_APPROVE:true}   # 개발/발표 기본 true, 운영 false
```

### admin 통합 시점 (해소 절차)

admin-service의 승인 endpoint 도입 시:
1. `application.yml`: `rider.auto-approve: false`
2. `RiderService.join()` 내 임시 블록 제거 (TODO 주석 trigger)
3. PENDING 상태 라이더는 admin approve 후 ACTIVE 전환 — interfaces.md §3.1 그대로 활용
4. tech-debt 해소 표시 — 본 D11 임시 처리 완료

**중요**: 이 임시 처리는 **Q2-C(자동 승인) 결정 변경이 아닌 Q2-B 흐름의 임시 우회**.
PENDING 상태 자체는 정상 저장되어 admin endpoint 도입 시 즉시 통합 가능.

### tech-debt 등재

- D11 임시 처리 — admin-service approve endpoint 도입 후 `rider.auto-approve: false` 전환 + 임시 블록 제거 (Phase 5 admin-service 진행 동기화 시점)

---

## 관련 메모리

- `feedback_dead_config_avoidance.md` — 사가 인프라 도입 보류 근거
- `feedback_verify_diagnostic_assumptions.md` — Role enum 부재 검증 (정정 #6)
- `project_phase4a_backfill_state.md` — InternalUserController 패턴 재사용 근거
- `project_phase_rider_cleanup_state.md` — D11 결정 박제 (admin 의존성 우회)
