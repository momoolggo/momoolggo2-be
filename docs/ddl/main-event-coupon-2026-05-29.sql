USE my_mmg_main;

CREATE TABLE IF NOT EXISTS coupon_reward_code (
    reward_code_id BIGINT NOT NULL AUTO_INCREMENT,
    user_no BIGINT NOT NULL,
    event_code VARCHAR(50) NOT NULL,
    reward_stage INT NOT NULL,
    coupon_id BIGINT NOT NULL,
    code VARCHAR(30) NOT NULL,
    issue_date DATE NOT NULL,
    expires_at DATETIME NOT NULL,
    is_used TINYINT(1) NOT NULL DEFAULT 0,
    used_at DATETIME DEFAULT NULL,
    couponlist_id BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (reward_code_id),
    UNIQUE KEY uq_coupon_reward_code_code (code),
    UNIQUE KEY uq_coupon_reward_code_user_event_date (user_no, event_code, issue_date),
    KEY idx_coupon_reward_code_user_code (user_no, code),
    KEY idx_coupon_reward_code_couponlist (couponlist_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_couponlist_user_coupon_usable
ON couponlist (user_no, coupon_id, is_used, order_id, expires_at);

INSERT INTO coupon (
    name,
    discount_type,
    discount_value,
    min_discount_amount,
    max_discount_amount,
    total_count,
    remaining_count,
    issue_start_date,
    issue_end_date,
    validity_days,
    issue_type,
    is_active,
    description
)
SELECT
    '푸드 챌린지 5% 할인쿠폰',
    'PERCENT',
    5,
    5000,
    5000,
    NULL,
    NULL,
    CURDATE(),
    '2099-12-31',
    30,
    'EVENT',
    TRUE,
    'FOOD_CUP_STAGE_1'
WHERE NOT EXISTS (
    SELECT 1 FROM coupon
    WHERE issue_type = 'EVENT'
      AND description = 'FOOD_CUP_STAGE_1'
);

INSERT INTO coupon (
    name,
    discount_type,
    discount_value,
    min_discount_amount,
    max_discount_amount,
    total_count,
    remaining_count,
    issue_start_date,
    issue_end_date,
    validity_days,
    issue_type,
    is_active,
    description
)
SELECT
    '푸드 챌린지 10% 할인쿠폰',
    'PERCENT',
    10,
    5000,
    5000,
    NULL,
    NULL,
    CURDATE(),
    '2099-12-31',
    30,
    'EVENT',
    TRUE,
    'FOOD_CUP_STAGE_2'
WHERE NOT EXISTS (
    SELECT 1 FROM coupon
    WHERE issue_type = 'EVENT'
      AND description = 'FOOD_CUP_STAGE_2'
);

INSERT INTO coupon (
    name,
    discount_type,
    discount_value,
    min_discount_amount,
    max_discount_amount,
    total_count,
    remaining_count,
    issue_start_date,
    issue_end_date,
    validity_days,
    issue_type,
    is_active,
    description
)
SELECT
    '푸드 챌린지 15% 할인쿠폰',
    'PERCENT',
    15,
    5000,
    5000,
    NULL,
    NULL,
    CURDATE(),
    '2099-12-31',
    30,
    'EVENT',
    TRUE,
    'FOOD_CUP_STAGE_3'
WHERE NOT EXISTS (
    SELECT 1 FROM coupon
    WHERE issue_type = 'EVENT'
      AND description = 'FOOD_CUP_STAGE_3'
);

INSERT INTO coupon (
    name,
    discount_type,
    discount_value,
    min_discount_amount,
    max_discount_amount,
    total_count,
    remaining_count,
    issue_start_date,
    issue_end_date,
    validity_days,
    issue_type,
    is_active,
    description
)
SELECT
    '푸드 챌린지 음료 무료 쿠폰',
    'FIXED',
    2000,
    10000,
    2000,
    NULL,
    NULL,
    CURDATE(),
    '2099-12-31',
    30,
    'EVENT',
    TRUE,
    'FOOD_CUP_STAGE_4'
WHERE NOT EXISTS (
    SELECT 1 FROM coupon
    WHERE issue_type = 'EVENT'
      AND description = 'FOOD_CUP_STAGE_4'
);

INSERT INTO coupon (
    name,
    discount_type,
    discount_value,
    min_discount_amount,
    max_discount_amount,
    total_count,
    remaining_count,
    issue_start_date,
    issue_end_date,
    validity_days,
    issue_type,
    is_active,
    description
)
SELECT
    '푸드 챌린지 전설 20% 할인쿠폰',
    'FIXED',
    10000,
    15000,
    10000,
    NULL,
    NULL,
    CURDATE(),
    '2099-12-31',
    30,
    'EVENT',
    TRUE,
    'FOOD_CUP_STAGE_5'
WHERE NOT EXISTS (
    SELECT 1 FROM coupon
    WHERE issue_type = 'EVENT'
      AND description = 'FOOD_CUP_STAGE_5'
);
