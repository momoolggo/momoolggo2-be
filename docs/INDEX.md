# docs/ 인벤토리

> 프로젝트 자료 모음. 새 파일 추가 시 이 인덱스에도 한 줄 설명을 같이 추가합니다.

---

## 📋 핵심 문서 (루트)

| 파일 | 설명 |
|---|---|
| [migration-plan.md](migration-plan.md) | MSA 전환 Phase 0~6 체크리스트 (현재 진행 위치 표시) |
| [decisions.md](decisions.md) | 설계 의사결정 기록 (왜 그렇게 정했는지 누적) |
| [FRONTEND_CHANGES.md](FRONTEND_CHANGES.md) | 프론트(Vue) 작업자 대상 — MSA 전환 중 발생한 API/응답 변경사항 누적 |
| [current-structure.txt](current-structure.txt) | 기존 모놀리식 패키지 트리 (마이그레이션 시 참조) |
| [original-application.yml](original-application.yml) | 기존 MA의 application.yaml 백업 |
| [original-build.gradle](original-build.gradle) | 기존 MA의 build.gradle 백업 |

## 📐 명세서

| 폴더/파일 | 설명 |
|---|---|
| [api-spec/api-spec.md](api-spec/api-spec.md) | API 명세서 — 전체 148개 (공통 9 / 고객 45 / 사장 23 / 라이더 14 / 관리자 39 / 서버간 18) |
| [requirements/requirements.md](requirements/requirements.md) | 요구사항 명세서 — 전체 80개 (기능 72 + 비기능 8) |
| [requirements/functions.md](requirements/functions.md) | 기능 명세서 — 사용자 시나리오 기반 기능 정의 (공통/라이더/관리자/사장/고객) |

## 🗂️ 도식 / 디자인 (자료 누적용)

| 폴더 | 용도 |
|---|---|
| `erd/` | ERD 다이어그램 (현재 PNG 1개) |
| `architecture/` | 시스템 아키텍처 다이어그램 |
| `flowcharts/` | 플로우차트 (주문/결제/배달 등) |
| `ddl/` | DB DDL 스크립트 |
| `figma/` | Figma 화면 디자인 export |

---

## 🔗 Phase별 참고 자료 매핑

각 Phase에서 어떤 자료를 먼저 봐야 하는지 가이드.

| Phase | 우선 참고 자료 | 비고 |
|---|---|---|
| **Phase 0** (스켈레톤) | `decisions.md` (모듈 구성), `current-structure.txt` | 골격만 잡으므로 명세서는 후순위 |
| **Phase 1** (Auth 분리) | `api-spec.md` (공통/고객 인증 부분), `requirements.md` (회원/약관), `current-structure.txt` (user/address 코드 위치) | 기존 API 응답 스펙 동결 검증 |
| **Phase 2** (Main 도메인 이동) | `api-spec.md` (고객/사장), `current-structure.txt` (store/order/cart/payment), `erd/` | 도메인별 기존 코드 위치 정확히 파악 |
| **Phase 3** (JPA 마이그레이션) | `current-structure.txt`, `api-spec.md` 응답 구조 | 응답 스펙 변경 없는지 검증 필수 |
| **Phase 4** (FeignClient/Gateway) | `api-spec.md` (서버간통신 18개), `decisions.md` (논리 FK) | 서비스 경계 확정 |
| **Phase 5** (신규 기능) | `requirements.md` (펫/챗봇/룰렛/쿠폰), `functions.md` (사용자 시나리오), `api-spec.md` 신규 항목 | 명세서 → 구현 순 |
| **Phase 6** (고도화) | `decisions.md` 향후 결정 항목 | 모니터링/CI/CD 선택 |

---

## 📌 참고 규칙

- 새 자료를 추가했으면 **이 INDEX부터 갱신** (PR 단위로 묶기)
- 외부 링크(Notion/Figma URL 등)도 여기에 정리
- Phase별 매핑은 진행 중 발견되는 자료 변화에 따라 업데이트
