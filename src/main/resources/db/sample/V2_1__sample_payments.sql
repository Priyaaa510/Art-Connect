-- Sample data for payments (runs after V2__payments.sql)
-- Marigold Morning: paid and delivered. Signal & Noise: paid, waiting for the artist to ship.
-- Northern Lights, Tromsø: auction won by aisha_khan, still awaiting her payment (left as V2 set it).

UPDATE orders
SET status = 'PAID', payment_provider = 'mock', payment_id = 'demo_pay_signal_noise', paid_at = created_at
WHERE artwork_title = 'Signal & Noise';

UPDATE artworks SET status = 'SOLD' WHERE title = 'Signal & Noise';

UPDATE orders
SET payment_provider = 'mock', payment_id = 'demo_pay_marigold', paid_at = created_at
WHERE artwork_title = 'Marigold Morning';
