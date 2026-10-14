-- Payment gateway support: orders are paid online before the artist ships them.

-- Order lifecycle: AWAITING_PAYMENT -> PAID -> COMPLETED, or CANCELLED before payment.
-- Orders created before payments existed ("PENDING") now wait for the buyer to pay.
ALTER TABLE orders DROP CONSTRAINT chk_orders_status;
ALTER TABLE orders ALTER COLUMN status SET DATA TYPE VARCHAR(20);
UPDATE orders SET status = 'AWAITING_PAYMENT' WHERE status = 'PENDING';
ALTER TABLE orders ADD CONSTRAINT chk_orders_status
    CHECK (status IN ('AWAITING_PAYMENT', 'PAID', 'COMPLETED', 'CANCELLED'));

ALTER TABLE orders ADD COLUMN payment_provider VARCHAR(20);
ALTER TABLE orders ADD COLUMN provider_order_id VARCHAR(64);
ALTER TABLE orders ADD COLUMN payment_id VARCHAR(64);
ALTER TABLE orders ADD COLUMN paid_at TIMESTAMP WITH TIME ZONE;
-- When an unpaid reservation is released (NULL = no automatic expiry, e.g. auction wins)
ALTER TABLE orders ADD COLUMN hold_expires_at TIMESTAMP WITH TIME ZONE;

CREATE UNIQUE INDEX ux_orders_provider_order ON orders (provider_order_id);
CREATE INDEX idx_orders_status_hold ON orders (status, hold_expires_at);

-- Artworks are RESERVED while their buyer pays
ALTER TABLE artworks DROP CONSTRAINT chk_artworks_status;
UPDATE artworks SET status = 'RESERVED'
WHERE id IN (SELECT artwork_id FROM orders WHERE status = 'AWAITING_PAYMENT');
ALTER TABLE artworks ADD CONSTRAINT chk_artworks_status CHECK (status IN ('AVAILABLE', 'RESERVED', 'SOLD'));
