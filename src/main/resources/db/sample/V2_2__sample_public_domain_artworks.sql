-- =====================================================================================
-- Replaces the sample artworks' illustrations with photographs of famous public-domain
-- artworks (see static/images/samples/CREDITS.md), and updates titles, details, tags,
-- reviews, orders and exhibitions to match. Prices, sale types, bids and statuses are kept.
--
-- The demo sellers are presented as dealers: they did not create these works, and every
-- listing credits the original artist.
-- =====================================================================================

-- ---------- Artworks ----------
UPDATE artworks a
SET title         = v.new_title,
    description   = v.description,
    category      = v.category,
    medium        = v.medium,
    dimensions    = v.dimensions,
    creation_year = v.creation_year,
    image_url     = v.image_url
FROM (VALUES
    ('Monsoon over Jaipur', 'Paris Street; Rainy Day',
     'Gustave Caillebotte''s monumental view of a Paris crossroads in the rain: umbrellas, wet cobblestones and the new boulevards of Haussmann''s city, painted with almost photographic precision.'
     || ' Image: Gustave Caillebotte, 1877, public domain (Art Institute of Chicago). Demo listing.',
     'Painting', 'Oil on canvas', '212.2 x 276.2 cm', 1877, '/images/samples/paris-street-rainy-day.jpg'),
    ('Marigold Morning', 'Water Lilies',
     'One of Claude Monet''s celebrated water-lily canvases from his garden at Giverny, where the surface of the pond becomes a mirror of sky, cloud and blossom.'
     || ' Image: Claude Monet, 1906, public domain (Art Institute of Chicago). Demo listing.',
     'Painting', 'Oil on canvas', '89.9 x 94.1 cm', 1906, '/images/samples/water-lilies.jpg'),
    ('The Blue City at Dusk', 'Nocturne: Blue and Gold - Southampton Water',
     'Whistler turns a harbour at twilight into a near-abstract harmony of blue and gold, with ships and lights dissolving into the evening haze.'
     || ' Image: James McNeill Whistler, 1872, public domain (Art Institute of Chicago). Demo listing.',
     'Painting', 'Oil on canvas', NULL, 1872, '/images/samples/nocturne-blue-and-gold.jpg'),

    ('Circuit Bloom', 'Improvisation No. 30 (Cannons)',
     'Painted on the eve of the First World War, Kandinsky''s improvisation bursts with colour and energy, with firing cannons hidden among the swirling forms.'
     || ' Image: Vasily Kandinsky, 1913, public domain (Art Institute of Chicago). Demo listing.',
     'Painting', 'Oil on canvas', NULL, 1913, '/images/samples/improvisation-no-30.jpg'),
    ('Neon Monsoon', 'Painting with Green Center',
     'An early milestone of abstract art: floating fields of colour gathered around a luminous green heart.'
     || ' Image: Vasily Kandinsky, 1913, public domain (Art Institute of Chicago). Demo listing.',
     'Painting', 'Oil on canvas', NULL, 1913, '/images/samples/painting-with-green-center.jpg'),
    ('Signal & Noise', 'The Bedroom',
     'Van Gogh''s own room in the Yellow House in Arles, painted in flat, vivid colours to suggest, in his words, absolute rest.'
     || ' Image: Vincent van Gogh, 1889, public domain (Art Institute of Chicago). Demo listing.',
     'Painting', 'Oil on canvas', '73.6 x 92.3 cm', 1889, '/images/samples/the-bedroom.jpg'),
    ('Orbit Study No. 3', 'Street in Saintes-Maries-de-la-Mer',
     'A lively reed-pen drawing of cottages in a Mediterranean fishing village, made during Van Gogh''s trip to the coast in the summer of 1888.'
     || ' Image: Vincent van Gogh, 1888, CC0 (The Metropolitan Museum of Art). Demo listing.',
     'Drawing', 'Reed pen and ink on paper', NULL, 1888, '/images/samples/street-in-saintes-maries.jpg'),

    ('Spree at Blue Hour', 'River Scene, France',
     'Camille Silvy''s celebrated early landscape photograph of the river Huisne: still water, tall poplars and a stormy sky, printed in a soft oval.'
     || ' Image: Camille Silvy, 1858, public domain. Demo listing.',
     'Photography', 'Albumen silver print', NULL, 1858, '/images/samples/river-scene.jpg'),
    ('Concrete Poetry', 'The Great Wave off Kanagawa',
     'Hokusai''s iconic woodblock print from the series Thirty-six Views of Mount Fuji: a towering wave curls over fishing boats, with Fuji small and serene beyond.'
     || ' Image: Katsushika Hokusai, c. 1830-32, public domain. Demo listing.',
     'Printmaking', 'Woodblock print; ink and colour on paper', NULL, 1831, '/images/samples/the-great-wave.jpg'),
    ('Northern Lights, Tromsø', 'Mrs. Herbert Duckworth (Julia Jackson)',
     'A luminous portrait by the pioneering photographer Julia Margaret Cameron of her niece Julia Jackson, later the mother of Virginia Woolf.'
     || ' Image: Julia Margaret Cameron, 1867, public domain. Demo listing.',
     'Photography', 'Albumen silver print', NULL, 1867, '/images/samples/julia-jackson.jpg'),

    ('Kente Geometry', 'Woven Silk Panel',
     'Fragments of a 15th-century silk panel woven with bands of interlacing stars and stylised motifs in gold, crimson and blue.'
     || ' Image: silk panel, 15th century, CC0 (The Metropolitan Museum of Art). Demo listing.',
     'Textile Art', 'Silk', NULL, NULL, '/images/samples/textile-panel.jpg'),
    ('Ancestors in Bronze', 'The Thinker',
     'Rodin''s famous figure, first conceived for The Gates of Hell, sits lost in thought, every muscle tensed with concentration.'
     || ' Image: Auguste Rodin, modeled 1880, CC0 (The Metropolitan Museum of Art). Demo listing.',
     'Sculpture', 'Bronze', NULL, 1880, '/images/samples/the-thinker.jpg'),
    ('Market Day, Makola', 'A Sunday on La Grande Jatte',
     'Seurat''s masterpiece of pointillism: Parisians relaxing on an island in the Seine, built from countless tiny dots of pure colour.'
     || ' Image: Georges Seurat, 1884-86, public domain (Art Institute of Chicago). Demo listing.',
     'Painting', 'Oil on canvas', '207.5 x 308.1 cm', 1884, '/images/samples/la-grande-jatte.jpg'),
    ('Tidal Forms', 'The Burghers of Calais',
     'Six citizens of Calais offer their lives to save their besieged city. Rodin''s moving group shows each man facing his fate alone.'
     || ' Image: Auguste Rodin, modeled 1884-95, CC0 (The Metropolitan Museum of Art). Demo listing.',
     'Sculpture', 'Bronze', NULL, 1889, '/images/samples/burghers-of-calais.jpg')
) AS v(old_title, new_title, description, category, medium, dimensions, creation_year, image_url)
WHERE a.title = v.old_title AND a.image_url LIKE '/images/samples/%';

-- ---------- Tags ----------
DELETE FROM artwork_tags
WHERE artwork_id IN (SELECT id FROM artworks WHERE image_url LIKE '/images/samples/%.jpg');

INSERT INTO artwork_tags (artwork_id, tag)
SELECT a.id, v.tag
FROM (VALUES
    ('Paris Street; Rainy Day', 'impressionism'), ('Paris Street; Rainy Day', 'cityscape'), ('Paris Street; Rainy Day', 'rain'),
    ('Water Lilies', 'impressionism'), ('Water Lilies', 'flowers'), ('Water Lilies', 'water'),
    ('Nocturne: Blue and Gold - Southampton Water', 'night'), ('Nocturne: Blue and Gold - Southampton Water', 'water'), ('Nocturne: Blue and Gold - Southampton Water', 'blue'),
    ('Improvisation No. 30 (Cannons)', 'abstract'), ('Improvisation No. 30 (Cannons)', 'modernism'),
    ('Painting with Green Center', 'abstract'), ('Painting with Green Center', 'modernism'),
    ('The Bedroom', 'post-impressionism'), ('The Bedroom', 'interior'),
    ('Street in Saintes-Maries-de-la-Mer', 'village'), ('Street in Saintes-Maries-de-la-Mer', 'ink'),
    ('River Scene, France', 'landscape'), ('River Scene, France', 'water'), ('River Scene, France', 'early photography'),
    ('The Great Wave off Kanagawa', 'japan'), ('The Great Wave off Kanagawa', 'ocean'), ('The Great Wave off Kanagawa', 'woodblock'),
    ('Mrs. Herbert Duckworth (Julia Jackson)', 'portrait'), ('Mrs. Herbert Duckworth (Julia Jackson)', 'early photography'),
    ('Woven Silk Panel', 'pattern'), ('Woven Silk Panel', 'silk'),
    ('The Thinker', 'bronze'), ('The Thinker', 'figure'),
    ('A Sunday on La Grande Jatte', 'pointillism'), ('A Sunday on La Grande Jatte', 'people'), ('A Sunday on La Grande Jatte', 'water'),
    ('The Burghers of Calais', 'bronze'), ('The Burghers of Calais', 'figure')
) AS v(title, tag)
JOIN artworks a ON a.title = v.title AND a.image_url LIKE '/images/samples/%';

-- ---------- Orders keep a snapshot of the artwork they were for ----------
UPDATE orders o
SET artwork_title = a.title, image_url = a.image_url
FROM artworks a
WHERE o.artwork_id = a.id AND a.image_url LIKE '/images/samples/%.jpg';

-- ---------- Reviews that described the old illustrations ----------
UPDATE reviews r
SET comment = v.comment
FROM (VALUES
    ('Improvisation No. 30 (Cannons)', 'meera_kapoor', 'Kandinsky''s colour still feels explosive more than a century later.'),
    ('The Bedroom', 'daniel_lee', 'The colours are even calmer in person. A joy to live with.'),
    ('River Scene, France', 'daniel_lee', 'The reflections on the water are stunning for an 1850s photograph.'),
    ('River Scene, France', 'arjun_rao', 'Beautiful oval composition and such soft, warm tones.'),
    ('The Great Wave off Kanagawa', 'kofi_mensah', 'That claw-like crest never gets old. A superb impression.'),
    ('Woven Silk Panel', 'lena_fischer', 'The interlacing geometry is mesmerising up close.')
) AS v(title, username, comment)
JOIN artworks a ON a.title = v.title
JOIN app_users u ON u.username = v.username
WHERE r.artwork_id = a.id AND r.author_id = u.id;

-- ---------- Sellers are dealers of these works ----------
UPDATE app_users u
SET bio = v.bio
FROM (VALUES
    ('meera_kapoor', 'Jaipur-based dealer specialising in Impressionist painting and the art of weather and water.'),
    ('arjun_rao', 'Specialist in Post-Impressionism and early abstraction, from Van Gogh to Kandinsky.'),
    ('lena_fischer', 'Berlin dealer in early photography and Japanese woodblock prints.'),
    ('kofi_mensah', 'Sculpture and historic textiles, from Rodin''s bronzes to 15th-century silks.')
) AS v(username, bio)
WHERE u.username = v.username;

UPDATE app_users
SET bio = bio || ' (Demo seller: listings show public-domain artworks as sample images.)'
WHERE username IN ('meera_kapoor', 'arjun_rao', 'lena_fischer', 'kofi_mensah');

-- ---------- Exhibitions ----------
UPDATE exhibitions e
SET title = v.new_title, description = v.description
FROM (VALUES
    ('Monsoon Moods', 'Rain, Water & Light',
     'Impressionist visions of weather and water: Caillebotte''s rain-soaked Paris, Whistler''s twilight harbour and Monet''s water lilies.'),
    ('Signals: Digital Futures', 'The Colour Revolution',
     'From Van Gogh''s Arles to Kandinsky''s first abstractions: how colour broke free of description and became the subject itself.'),
    ('Northern Light', 'Silver & Light: Early Photography',
     'The first great photographers: Camille Silvy''s river landscapes and Julia Margaret Cameron''s luminous portraits.'),
    ('Form & Fabric', 'Form & Figure',
     'Rodin''s bronzes alongside a 15th-century woven silk and Seurat''s Sunday crowd: a study of the human figure and pattern.')
) AS v(old_title, new_title, description)
WHERE e.title = v.old_title;
