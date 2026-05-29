-- Order lookup index additions
-- Date: 2026-05-29
-- Schema: my_mmg_main
-- Target table: orders
-- Scope: DB index only. No Java API response or mapper logic changes.

USE my_mmg_main;

-- 1) Customer order history lookup:
--    WHERE user_no = ? ORDER BY order_time DESC
CREATE INDEX idx_orders_user_time
ON orders (user_no, order_time DESC);

-- 2) Owner order list/status lookup:
--    WHERE store_id = ? AND pay_state = 2 [AND order_state = ?]
--    ORDER BY order_time DESC
CREATE INDEX idx_orders_store_pay_state_time
ON orders (store_id, pay_state, order_state, order_time DESC);

-- 3) Store period/settlement lookup:
--    WHERE store_id = ? AND order_time >= ? AND order_time < ?
CREATE INDEX idx_orders_store_time
ON orders (store_id, order_time DESC);
