USE my_mmg_main;

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
    '친환경 새싹 1,000원 할인 쿠폰',
    'FIXED',
    1000,
    0,
    1000,
    NULL,
    NULL,
    CURDATE(),
    '2099-12-31',
    30,
    'ECO_LEVEL',
    TRUE,
    'ECO_LEVEL_SPROUT_1000'
WHERE NOT EXISTS (
    SELECT 1 FROM coupon
    WHERE name = '친환경 새싹 1,000원 할인 쿠폰'
      AND issue_type = 'ECO_LEVEL'
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
    '친환경 나무 3,000원 할인 쿠폰',
    'FIXED',
    3000,
    0,
    3000,
    NULL,
    NULL,
    CURDATE(),
    '2099-12-31',
    30,
    'ECO_LEVEL',
    TRUE,
    'ECO_LEVEL_3_3000'
WHERE NOT EXISTS (
    SELECT 1 FROM coupon
    WHERE name = '친환경 나무 3,000원 할인 쿠폰'
      AND issue_type = 'ECO_LEVEL'
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
    '친환경 숲 5,000원 할인 쿠폰',
    'FIXED',
    5000,
    0,
    5000,
    NULL,
    NULL,
    CURDATE(),
    '2099-12-31',
    30,
    'ECO_LEVEL',
    TRUE,
    'ECO_LEVEL_4_5000'
WHERE NOT EXISTS (
    SELECT 1 FROM coupon
    WHERE name = '친환경 숲 5,000원 할인 쿠폰'
      AND issue_type = 'ECO_LEVEL'
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
    '친환경 지구 7,000원 할인 쿠폰',
    'FIXED',
    7000,
    0,
    7000,
    NULL,
    NULL,
    CURDATE(),
    '2099-12-31',
    30,
    'ECO_LEVEL',
    TRUE,
    'ECO_LEVEL_5_7000'
WHERE NOT EXISTS (
    SELECT 1 FROM coupon
    WHERE name = '친환경 지구 7,000원 할인 쿠폰'
      AND issue_type = 'ECO_LEVEL'
);
