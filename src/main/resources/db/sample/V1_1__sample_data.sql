-- =====================================================================================
-- ArtConnect sample data (PostgreSQL)
--
-- Flyway runs this once, right after the schema (V1), so a fresh database starts with
-- a populated gallery. Every account's password is: password123
--
--   admin          ADMIN      moderation account
--   meera_kapoor   ARTIST     painter, Jaipur
--   arjun_rao      ARTIST     digital artist, Bengaluru
--   lena_fischer   ARTIST     photographer & printmaker, Berlin
--   kofi_mensah    ARTIST     sculptor & textile artist, Accra
--   priya_sharma   COLLECTOR
--   daniel_lee     COLLECTOR
--   aisha_khan     COLLECTOR
--
-- Dates are relative to now(), so auctions are live and exhibitions are current on
-- whatever day the database is created. Images live in static/images/samples.
--
-- To skip this data (e.g. in production), set FLYWAY_LOCATIONS=classpath:db/migration.
-- =====================================================================================

-- ---------- Users ----------
INSERT INTO app_users (username, password_hash, role, display_name, bio, location, website, created_at) VALUES
('admin',        '$2a$10$9tq1P19jpRlEJmZ.6GMR7.hHC8T/OoNH9bzuzDfH07xz9Wc3UO1ty', 'ADMIN',     'Gallery Admin',
 'Keeps the ArtConnect gallery running smoothly.', NULL, NULL, now() - interval '120 days'),
('meera_kapoor', '$2a$10$9tq1P19jpRlEJmZ.6GMR7.hHC8T/OoNH9bzuzDfH07xz9Wc3UO1ty', 'ARTIST',    'Meera Kapoor',
 'Painter from Jaipur. I work in oils and acrylics, chasing the light of the monsoon and the colours of Rajasthan''s old cities.',
 'Jaipur, India', 'https://example.com/meera-kapoor', now() - interval '95 days'),
('arjun_rao',    '$2a$10$9tq1P19jpRlEJmZ.6GMR7.hHC8T/OoNH9bzuzDfH07xz9Wc3UO1ty', 'ARTIST',    'Arjun Rao',
 'Generative and digital artist. By day I design circuits; by night I turn them into flowers, storms and signals.',
 'Bengaluru, India', NULL, now() - interval '80 days'),
('lena_fischer', '$2a$10$9tq1P19jpRlEJmZ.6GMR7.hHC8T/OoNH9bzuzDfH07xz9Wc3UO1ty', 'ARTIST',    'Lena Fischer',
 'Photographer and printmaker. Blue hours in Berlin, long nights in the Arctic, and the quiet poetry of concrete.',
 'Berlin, Germany', 'https://example.com/lena-fischer', now() - interval '70 days'),
('kofi_mensah',  '$2a$10$9tq1P19jpRlEJmZ.6GMR7.hHC8T/OoNH9bzuzDfH07xz9Wc3UO1ty', 'ARTIST',    'Kofi Mensah',
 'Sculptor and textile artist. My work draws on kente weaving, market life and the forms the sea leaves behind.',
 'Accra, Ghana', NULL, now() - interval '60 days'),
('priya_sharma', '$2a$10$9tq1P19jpRlEJmZ.6GMR7.hHC8T/OoNH9bzuzDfH07xz9Wc3UO1ty', 'COLLECTOR', 'Priya Sharma',
 'Collector of contemporary Indian painting.', 'Mumbai, India', NULL, now() - interval '50 days'),
('daniel_lee',   '$2a$10$9tq1P19jpRlEJmZ.6GMR7.hHC8T/OoNH9bzuzDfH07xz9Wc3UO1ty', 'COLLECTOR', 'Daniel Lee',
 'Digital art enthusiast and occasional bidder.', 'Singapore', NULL, now() - interval '45 days'),
('aisha_khan',   '$2a$10$9tq1P19jpRlEJmZ.6GMR7.hHC8T/OoNH9bzuzDfH07xz9Wc3UO1ty', 'COLLECTOR', 'Aisha Khan',
 'Architect who collects photography and sculpture.', 'Dubai, UAE', NULL, now() - interval '40 days');

-- ---------- Artworks ----------
-- Fixed-price pieces are AVAILABLE or SOLD; auctions have an end time (three live, one finished).
INSERT INTO artworks (artist_id, title, description, price, category, medium, dimensions, creation_year,
                      image_url, sale_type, status, auction_ends_at, created_at)
SELECT u.id, v.title, v.description, v.price, v.category, v.medium, v.dimensions, v.creation_year,
       v.image_url, v.sale_type, v.status, v.auction_ends_at, v.created_at
FROM (VALUES
    ('meera_kapoor', 'Monsoon over Jaipur',
     'Rain sweeps across the hills as the old fort glows in the last light. Layered glazes build a sky that is heavy, bruised and beautiful.',
     18500.00, 'Painting', 'Oil on canvas', '90 x 60 cm', 2025,
     '/images/samples/monsoon-over-jaipur.svg', 'FIXED', 'AVAILABLE', NULL::timestamptz, now() - interval '30 days'),
    ('meera_kapoor', 'Marigold Morning',
     'A single marigold painted at dawn on Diwali morning, glowing like a small sun.',
     9500.00, 'Painting', 'Acrylic on board', '40 x 55 cm', 2024,
     '/images/samples/marigold-morning.svg', 'FIXED', 'SOLD', NULL, now() - interval '40 days'),
    ('meera_kapoor', 'The Blue City at Dusk',
     'Jodhpur''s indigo houses stacked below the fort as the sky turns from violet to peach.',
     12000.00, 'Painting', 'Oil on linen', '100 x 75 cm', 2025,
     '/images/samples/blue-city-at-dusk.svg', 'AUCTION', 'AVAILABLE', now() + interval '3 days 4 hours', now() - interval '4 days'),

    ('arjun_rao', 'Circuit Bloom',
     'A flower grown from printed circuit traces. Signals radiate from its centre like pollen.',
     6500.00, 'Digital Art', 'Archival giclee print', '60 x 60 cm', 2025,
     '/images/samples/circuit-bloom.svg', 'FIXED', 'AVAILABLE', NULL, now() - interval '20 days'),
    ('arjun_rao', 'Neon Monsoon',
     'Rain on a Bengaluru street at 2 a.m., where every sign bleeds into the puddles below.',
     5000.00, 'Digital Art', 'Digital painting, limited edition of 5', '80 x 60 cm', 2025,
     '/images/samples/neon-monsoon.svg', 'AUCTION', 'AVAILABLE', now() + interval '20 hours', now() - interval '3 days'),
    ('arjun_rao', 'Signal & Noise',
     'A minimalist study of information: the clean wave on the left dissolves into static on the right.',
     4200.00, 'Digital Art', 'Screen print', '50 x 40 cm', 2024,
     '/images/samples/signal-and-noise.svg', 'FIXED', 'SOLD', NULL, now() - interval '35 days'),
    ('arjun_rao', 'Orbit Study No. 3',
     'Hand-drawn orbits from a sketchbook series about planetary motion and chance.',
     3200.00, 'Drawing', 'Ink on paper', '30 x 30 cm', 2023,
     '/images/samples/orbit-study.svg', 'FIXED', 'AVAILABLE', NULL, now() - interval '15 days'),

    ('lena_fischer', 'Spree at Blue Hour',
     'The TV tower and the river Spree in the twenty minutes after sunset, when Berlin turns blue.',
     7800.00, 'Photography', 'Pigment print on Hahnemuehle paper', '70 x 50 cm', 2024,
     '/images/samples/spree-blue-hour.svg', 'FIXED', 'AVAILABLE', NULL, now() - interval '25 days'),
    ('lena_fischer', 'Concrete Poetry',
     'A linocut celebrating the rhythm of brutalist facades: windows as verses, walls as stanzas.',
     5400.00, 'Printmaking', 'Linocut, edition of 20', '42 x 59 cm', 2025,
     '/images/samples/concrete-poetry.svg', 'FIXED', 'AVAILABLE', NULL, now() - interval '12 days'),
    ('lena_fischer', 'Northern Lights, Tromsø',
     'Aurora over a snowbound cabin, photographed at minus twenty-two degrees.',
     6000.00, 'Photography', 'Pigment print', '90 x 60 cm', 2023,
     '/images/samples/northern-lights.svg', 'AUCTION', 'SOLD', now() - interval '1 day', now() - interval '8 days'),

    ('kofi_mensah', 'Kente Geometry',
     'A hand-woven panel reinterpreting traditional kente strip patterns in bold geometric blocks.',
     22000.00, 'Textile Art', 'Hand-woven silk and cotton', '120 x 90 cm', 2025,
     '/images/samples/kente-geometry.svg', 'FIXED', 'AVAILABLE', NULL, now() - interval '18 days'),
    ('kofi_mensah', 'Ancestors in Bronze',
     'A lost-wax bronze bust honouring the elders who carried stories across generations.',
     45000.00, 'Sculpture', 'Cast bronze on wooden plinth', '28 x 22 x 60 cm', 2025,
     '/images/samples/ancestors-in-bronze.svg', 'AUCTION', 'AVAILABLE', now() + interval '6 days', now() - interval '2 days'),
    ('kofi_mensah', 'Market Day, Makola',
     'Traders, fruit and fabric in the noon heat of Accra''s busiest market. Collage, paint and printed wax cloth.',
     15000.00, 'Mixed Media', 'Acrylic, collage and fabric on canvas', '100 x 75 cm', 2024,
     '/images/samples/market-day-makola.svg', 'FIXED', 'AVAILABLE', NULL, now() - interval '22 days'),
    ('kofi_mensah', 'Tidal Forms',
     'Two carved stones that rest against each other like waves meeting on the shore.',
     38000.00, 'Sculpture', 'Carved marble', '45 x 25 x 30 cm', 2024,
     '/images/samples/tidal-forms.svg', 'FIXED', 'AVAILABLE', NULL, now() - interval '10 days')
) AS v(username, title, description, price, category, medium, dimensions, creation_year,
       image_url, sale_type, status, auction_ends_at, created_at)
JOIN app_users u ON u.username = v.username;

INSERT INTO artwork_tags (artwork_id, tag)
SELECT a.id, v.tag
FROM (VALUES
    ('Monsoon over Jaipur', 'monsoon'), ('Monsoon over Jaipur', 'landscape'), ('Monsoon over Jaipur', 'rajasthan'),
    ('Marigold Morning', 'flowers'), ('Marigold Morning', 'still life'),
    ('The Blue City at Dusk', 'cityscape'), ('The Blue City at Dusk', 'rajasthan'), ('The Blue City at Dusk', 'blue'),
    ('Circuit Bloom', 'generative'), ('Circuit Bloom', 'flowers'), ('Circuit Bloom', 'neon'),
    ('Neon Monsoon', 'cityscape'), ('Neon Monsoon', 'neon'), ('Neon Monsoon', 'monsoon'),
    ('Signal & Noise', 'minimal'), ('Signal & Noise', 'abstract'),
    ('Orbit Study No. 3', 'minimal'), ('Orbit Study No. 3', 'space'),
    ('Spree at Blue Hour', 'cityscape'), ('Spree at Blue Hour', 'berlin'), ('Spree at Blue Hour', 'blue'),
    ('Concrete Poetry', 'architecture'), ('Concrete Poetry', 'abstract'),
    ('Northern Lights, Tromsø', 'landscape'), ('Northern Lights, Tromsø', 'night sky'),
    ('Kente Geometry', 'pattern'), ('Kente Geometry', 'ghana'),
    ('Ancestors in Bronze', 'portrait'), ('Ancestors in Bronze', 'ghana'),
    ('Market Day, Makola', 'people'), ('Market Day, Makola', 'ghana'),
    ('Tidal Forms', 'abstract'), ('Tidal Forms', 'ocean')
) AS v(title, tag)
JOIN artworks a ON a.title = v.title;

-- ---------- Bids ----------
-- Each bid respects the 5% minimum increment used by the app.
INSERT INTO bids (artwork_id, bidder_id, amount, created_at)
SELECT a.id, u.id, v.amount, v.created_at
FROM (VALUES
    ('The Blue City at Dusk',   'priya_sharma', 12000.00, now() - interval '3 days'),
    ('The Blue City at Dusk',   'daniel_lee',   12600.00, now() - interval '2 days'),
    ('The Blue City at Dusk',   'priya_sharma', 13300.00, now() - interval '10 hours'),
    ('Neon Monsoon',            'aisha_khan',    5000.00, now() - interval '2 days 6 hours'),
    ('Neon Monsoon',            'daniel_lee',    5250.00, now() - interval '2 days'),
    ('Neon Monsoon',            'aisha_khan',    5600.00, now() - interval '1 day'),
    ('Neon Monsoon',            'daniel_lee',    6000.00, now() - interval '3 hours'),
    ('Ancestors in Bronze',     'aisha_khan',   45000.00, now() - interval '1 day'),
    ('Northern Lights, Tromsø', 'daniel_lee',    6000.00, now() - interval '6 days'),
    ('Northern Lights, Tromsø', 'aisha_khan',    6500.00, now() - interval '4 days'),
    ('Northern Lights, Tromsø', 'daniel_lee',    7000.00, now() - interval '3 days'),
    ('Northern Lights, Tromsø', 'aisha_khan',    7400.00, now() - interval '2 days')
) AS v(title, username, amount, created_at)
JOIN artworks a ON a.title = v.title
JOIN app_users u ON u.username = v.username;

-- Current bid, leader and bid count follow from the bids above
UPDATE artworks a
SET current_bid = top.amount,
    highest_bidder_id = top.bidder_id,
    bid_count = top.bid_count
FROM (
    SELECT DISTINCT ON (artwork_id) artwork_id, bidder_id, amount,
           count(*) OVER (PARTITION BY artwork_id) AS bid_count
    FROM bids
    ORDER BY artwork_id, amount DESC
) top
WHERE a.id = top.artwork_id;

-- ---------- Orders ----------
INSERT INTO orders (artwork_id, buyer_id, artist_id, artwork_title, image_url, price, source, status, created_at, updated_at)
SELECT a.id, u.id, a.artist_id, a.title, a.image_url, v.price, v.source, v.status, v.created_at, v.updated_at
FROM (VALUES
    ('Marigold Morning',        'priya_sharma',  9500.00, 'PURCHASE', 'COMPLETED', now() - interval '28 days', now() - interval '21 days'),
    ('Signal & Noise',          'daniel_lee',    4200.00, 'PURCHASE', 'PENDING',   now() - interval '2 days',  now() - interval '2 days'),
    ('Northern Lights, Tromsø', 'aisha_khan',    7400.00, 'AUCTION',  'PENDING',   now() - interval '1 day',   now() - interval '1 day')
) AS v(title, username, price, source, status, created_at, updated_at)
JOIN artworks a ON a.title = v.title
JOIN app_users u ON u.username = v.username;

-- ---------- Reviews ----------
INSERT INTO reviews (artwork_id, author_id, rating, comment, created_at)
SELECT a.id, u.id, v.rating, v.comment, v.created_at
FROM (VALUES
    ('Monsoon over Jaipur', 'priya_sharma', 5, 'The sky in this painting is extraordinary. You can almost smell the rain.', now() - interval '20 days'),
    ('Monsoon over Jaipur', 'daniel_lee',   4, 'Beautiful mood and colour. Would love to see it in person.', now() - interval '15 days'),
    ('Marigold Morning',    'priya_sharma', 5, 'Arrived beautifully packed and it glows on my wall every morning.', now() - interval '20 days'),
    ('Circuit Bloom',       'aisha_khan',   4, 'Clever idea and the print quality is excellent.', now() - interval '12 days'),
    ('Circuit Bloom',       'meera_kapoor', 5, 'A perfect marriage of nature and technology.', now() - interval '10 days'),
    ('Signal & Noise',      'daniel_lee',   4, 'Crisp, minimal and a great conversation piece.', now() - interval '1 day'),
    ('Spree at Blue Hour',  'daniel_lee',   5, 'Captures that exact Berlin blue. Stunning.', now() - interval '14 days'),
    ('Spree at Blue Hour',  'arjun_rao',    4, 'Lovely composition and reflections on the water.', now() - interval '9 days'),
    ('Concrete Poetry',     'kofi_mensah',  4, 'Strong graphic rhythm. The red block is a great touch.', now() - interval '6 days'),
    ('Kente Geometry',      'priya_sharma', 5, 'The craftsmanship is incredible. Rich colours and a wonderful texture.', now() - interval '11 days'),
    ('Kente Geometry',      'lena_fischer', 5, 'Timeless pattern made thoroughly modern.', now() - interval '8 days'),
    ('Market Day, Makola',  'aisha_khan',   4, 'So much life and movement in this piece.', now() - interval '7 days')
) AS v(title, username, rating, comment, created_at)
JOIN artworks a ON a.title = v.title
JOIN app_users u ON u.username = v.username;

-- Rating summaries follow from the reviews above
UPDATE artworks a
SET average_rating = r.average_rating,
    review_count = r.review_count
FROM (
    SELECT artwork_id, avg(rating)::double precision AS average_rating, count(*) AS review_count
    FROM reviews
    GROUP BY artwork_id
) r
WHERE a.id = r.artwork_id;

-- ---------- Exhibitions ----------
INSERT INTO exhibitions (curator_id, title, description, start_date, end_date, created_at)
SELECT u.id, v.title, v.description, v.start_date, v.end_date, v.created_at
FROM (VALUES
    ('meera_kapoor', 'Monsoon Moods',
     'Paintings made during and after the rains: heavy skies, glowing cities and the colours that bloom when the storm has passed.',
     current_date - 7, current_date + 21, now() - interval '10 days'),
    ('arjun_rao', 'Signals: Digital Futures',
     'An exhibition about the beauty hidden inside technology, from circuit flowers to rain-soaked neon streets.',
     current_date - 3, current_date + 30, now() - interval '5 days'),
    ('lena_fischer', 'Northern Light',
     'Photographs from a winter north of the Arctic Circle, alongside Berlin in its bluest hour.',
     current_date - 60, current_date - 30, now() - interval '65 days'),
    ('kofi_mensah', 'Form & Fabric',
     'Bronze, marble and woven silk: a survey of sculpture and textile work rooted in West African tradition.',
     current_date + 10, current_date + 40, now() - interval '1 day')
) AS v(username, title, description, start_date, end_date, created_at)
JOIN app_users u ON u.username = v.username;

INSERT INTO exhibition_artworks (exhibition_id, artwork_id, position)
SELECT e.id, a.id, v.position
FROM (VALUES
    ('Monsoon Moods', 'Monsoon over Jaipur', 0),
    ('Monsoon Moods', 'The Blue City at Dusk', 1),
    ('Monsoon Moods', 'Marigold Morning', 2),
    ('Signals: Digital Futures', 'Neon Monsoon', 0),
    ('Signals: Digital Futures', 'Circuit Bloom', 1),
    ('Signals: Digital Futures', 'Signal & Noise', 2),
    ('Signals: Digital Futures', 'Orbit Study No. 3', 3),
    ('Northern Light', 'Northern Lights, Tromsø', 0),
    ('Northern Light', 'Spree at Blue Hour', 1),
    ('Form & Fabric', 'Kente Geometry', 0),
    ('Form & Fabric', 'Ancestors in Bronze', 1),
    ('Form & Fabric', 'Tidal Forms', 2),
    ('Form & Fabric', 'Market Day, Makola', 3)
) AS v(exhibition_title, artwork_title, position)
JOIN exhibitions e ON e.title = v.exhibition_title
JOIN artworks a ON a.title = v.artwork_title;
