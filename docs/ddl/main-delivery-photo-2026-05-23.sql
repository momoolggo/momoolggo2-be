-- 자잘 에러 트랙 #9-B (2026-05-23) — orders 테이블 delivered_photo_url 컬럼 추가
-- schema: my_mmg_main
-- 라이더 배달 완료 시 main /api/delivery-photo/upload 으로 저장된 URL을 박제.
-- 고객 OrderDetailView에서 orderState=6 완료 상태 시 표시.

ALTER TABLE `orders`
    ADD COLUMN `delivered_photo_url` VARCHAR(255) NULL
    COMMENT '배달 완료 사진 URL (예: /uploads/delivery/abc.jpg) — 자잘 에러 트랙 #9 (2026-05-23)';
