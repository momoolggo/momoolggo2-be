# 신고 누적 정확히 세기 검토

## 현재 흐름

- `POST /api/owner/review/{reviewId}/report`
- `mmg-main-service`에서 리뷰 존재 여부와 가게 사장 권한을 확인한다.
- `AdminFeignClient.reportReview`로 `mmg-admin-service`의 `/internal/report/review`를 호출한다.
- `ReportService`가 `report`를 저장하고, 커밋 후 `ReviewReportSubmittedEvent`로 AI 판정을 시작한다.
- AI 판정이 블라인드 대상이면 main-service 내부 블라인드 API를 호출하고 admin `blind` row를 생성한다.
- 기존 자동 제재는 `BlindScheduler`가 만료된 `BLINDED` row를 기준으로 `countByUserNo`를 세어 유지한다.

## 이번 수정 방향

- 같은 신고자가 같은 리뷰를 중복 신고하지 못하게 `ReportService`에서 선검사한다.
- DB race condition 방지를 위해 `reporter_no + target_type + target_no` unique DDL을 별도 기록한다.
- AI 자동 블라인드 처리 시 같은 리뷰에 active blind가 있으면 새 `blind` row를 만들지 않는다.
- 같은 리뷰 신고가 동시에 AI 처리될 때 같은 `targetNo`의 첫 report row를 비관적 락으로 잡아 blind 생성 구간을 직렬화한다.
- 기존 자동 제재 스케줄러 흐름은 유지한다.

## 검증 시나리오

- 같은 owner가 같은 reviewId로 두 번 신고하면 두 번째 요청은 409로 실패한다.
- 서로 다른 owner가 같은 reviewId를 신고해도 report 저장은 가능하다.
- 같은 reviewId에 대해 AI 처리 이벤트가 동시에 실행되어도 `blind` row는 1개만 생성된다.
- `BlindScheduler`는 기존처럼 `BLINDED` 만료 건 기준으로 1~2회 15일 정지, 3회 이상 영구 정지를 처리한다.
- main-service 신고 요청 payload에 `reviewContent`가 포함되어 admin AI 판정이 빈 본문 대신 실제 리뷰 내용을 사용할 수 있다.
