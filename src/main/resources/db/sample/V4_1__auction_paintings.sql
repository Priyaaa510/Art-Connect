-- =====================================================================================
-- Sample data: removes "The Kiss" and adds five paintings sold by auction, ending 20 days after the data is
-- loaded. Images are public-domain / CC0 photographs of the artworks (see static/images/samples/CREDITS.md).
-- =====================================================================================

-- Abandoned (cancelled) orders for The Kiss would otherwise linger in order history without their artwork
DELETE FROM orders
WHERE status = 'CANCELLED'
  AND artwork_id IN (SELECT id FROM artworks WHERE title = 'The Kiss' AND image_url = '/images/samples/the-kiss.jpg');

DELETE FROM checkouts c
WHERE c.status = 'CANCELLED'
  AND NOT EXISTS (SELECT 1 FROM orders o WHERE o.checkout_id = c.id);

-- Tags, bids, reviews, cart items and exhibition entries go with it (ON DELETE CASCADE)
DELETE FROM artworks WHERE title = 'The Kiss' AND image_url = '/images/samples/the-kiss.jpg';

INSERT INTO artworks (artist_id, title, description, price, category, medium, dimensions, creation_year,
                      image_url, sale_type, status, auction_ends_at, created_at)
SELECT u.id, v.title, v.description, v.price, v.category, v.medium, v.dimensions, v.creation_year,
       v.image_url, 'AUCTION', 'AVAILABLE', now() + interval '20 days', v.created_at
FROM (VALUES
    ('meera_kapoor', 'Woman with a Parasol',
     'Camille Monet and their son on a breezy summer hillside, seen from below against a bright sky: painted in a few quick hours outdoors.'
     || ' Image: Claude Monet, 1875, public domain (National Gallery of Art, Washington). Demo listing.',
     18000.00, 'Painting', 'Oil on canvas', '100 x 81 cm', 1875, '/images/samples/woman-with-a-parasol.jpg', now() - interval '2 days'),
    ('arjun_rao', 'Wheat Field with Cypresses',
     'Golden wheat ripples beneath flame-like cypresses and a swirling summer sky, painted near the asylum at Saint-Rémy.'
     || ' Image: Vincent van Gogh, 1889, CC0 (The Metropolitan Museum of Art). Demo listing.',
     20000.00, 'Painting', 'Oil on canvas', '73 x 93.4 cm', 1889, '/images/samples/wheat-field-with-cypresses.jpg', now() - interval '1 day'),
    ('lena_fischer', 'The Milkmaid',
     'A kitchen maid pours milk in a quiet, sunlit room, every crust of bread and brass rivet caught in Vermeer''s luminous light.'
     || ' Image: Johannes Vermeer, c. 1658, public domain (Rijksmuseum, Amsterdam). Demo listing.',
     22000.00, 'Painting', 'Oil on canvas', '45.5 x 41 cm', 1658, '/images/samples/the-milkmaid.jpg', now() - interval '3 days'),
    ('kofi_mensah', 'Wanderer above the Sea of Fog',
     'A lone traveller stands on a rocky summit, looking out over a landscape swallowed by fog: the emblem of Romanticism.'
     || ' Image: Caspar David Friedrich, c. 1818, public domain (Hamburger Kunsthalle). Demo listing.',
     17500.00, 'Painting', 'Oil on canvas', '94.8 x 74.8 cm', 1818, '/images/samples/wanderer-above-the-sea-of-fog.jpg', now() - interval '1 day'),
    ('meera_kapoor', 'Shakuntala',
     'Shakuntala pauses on a forest path, pretending to remove a thorn from her foot as she looks back for King Dushyanta.'
     || ' Image: Raja Ravi Varma, 1870, public domain. Demo listing.',
     15000.00, 'Painting', 'Oil on canvas', NULL, 1870, '/images/samples/shakuntala.jpg', now() - interval '4 days')
) AS v(username, title, description, price, category, medium, dimensions, creation_year, image_url, created_at)
JOIN app_users u ON u.username = v.username;

INSERT INTO artwork_tags (artwork_id, tag)
SELECT a.id, v.tag
FROM (VALUES
    ('Woman with a Parasol', 'impressionism'), ('Woman with a Parasol', 'portrait'), ('Woman with a Parasol', 'summer'),
    ('Wheat Field with Cypresses', 'post-impressionism'), ('Wheat Field with Cypresses', 'landscape'),
    ('The Milkmaid', 'dutch golden age'), ('The Milkmaid', 'interior'),
    ('Wanderer above the Sea of Fog', 'romanticism'), ('Wanderer above the Sea of Fog', 'landscape'), ('Wanderer above the Sea of Fog', 'mountains'),
    ('Shakuntala', 'indian art'), ('Shakuntala', 'mythology'), ('Shakuntala', 'portrait')
) AS v(title, tag)
JOIN artworks a ON a.title = v.title AND a.image_url LIKE '/images/samples/%';

-- A few opening bids, so some auctions already have a leader (each bid is at least 5% above the last)
INSERT INTO bids (artwork_id, bidder_id, amount, created_at)
SELECT a.id, u.id, v.amount, v.created_at
FROM (VALUES
    ('Woman with a Parasol',       'priya_sharma', 18000.00, now() - interval '1 day 20 hours'),
    ('Woman with a Parasol',       'daniel_lee',   18900.00, now() - interval '1 day 2 hours'),
    ('Wheat Field with Cypresses', 'aisha_khan',   20000.00, now() - interval '18 hours'),
    ('The Milkmaid',               'daniel_lee',   22000.00, now() - interval '2 days'),
    ('The Milkmaid',               'aisha_khan',   23100.00, now() - interval '1 day 12 hours'),
    ('The Milkmaid',               'daniel_lee',   24300.00, now() - interval '9 hours')
) AS v(title, username, amount, created_at)
JOIN artworks a ON a.title = v.title AND a.sale_type = 'AUCTION' AND a.image_url LIKE '/images/samples/%'
JOIN app_users u ON u.username = v.username;

UPDATE artworks a
SET current_bid = top.amount,
    highest_bidder_id = top.bidder_id,
    bid_count = top.bid_count
FROM (
    SELECT DISTINCT ON (b.artwork_id) b.artwork_id, b.bidder_id, b.amount,
           count(*) OVER (PARTITION BY b.artwork_id) AS bid_count
    FROM bids b
    JOIN artworks x ON x.id = b.artwork_id
    WHERE x.title IN ('Woman with a Parasol', 'Wheat Field with Cypresses', 'The Milkmaid')
    ORDER BY b.artwork_id, b.amount DESC
) top
WHERE a.id = top.artwork_id;
