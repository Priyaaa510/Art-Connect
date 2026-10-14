-- =====================================================================================
-- Ten more sample artworks, all AVAILABLE at a fixed price, so there is plenty to buy in a demo.
-- Images are public-domain / CC0 photographs of famous artworks (see static/images/samples/CREDITS.md);
-- the demo sellers are dealers and every listing credits the original artist.
-- =====================================================================================

INSERT INTO artworks (artist_id, title, description, price, category, medium, dimensions, creation_year,
                      image_url, sale_type, status, created_at)
SELECT u.id, v.title, v.description, v.price, v.category, v.medium, v.dimensions, v.creation_year,
       v.image_url, 'FIXED', 'AVAILABLE', v.created_at
FROM (VALUES
    ('meera_kapoor', 'Impression, Sunrise',
     'The painting that gave Impressionism its name: the port of Le Havre dissolving into mist as an orange sun rises over the water.'
     || ' Image: Claude Monet, 1872, public domain (Musée Marmottan Monet, Paris). Demo listing.',
     16500.00, 'Painting', 'Oil on canvas', '48 x 63 cm', 1872, '/images/samples/impression-sunrise.jpg', now() - interval '6 days'),
    ('meera_kapoor', 'Dance at Le Moulin de la Galette',
     'A sunlit Sunday afternoon in Montmartre: Parisians dancing, talking and flirting under the trees, dappled with light.'
     || ' Image: Pierre-Auguste Renoir, 1876, public domain (Musée d''Orsay, Paris). Demo listing.',
     21000.00, 'Painting', 'Oil on canvas', '131 x 175 cm', 1876, '/images/samples/moulin-de-la-galette.jpg', now() - interval '5 days'),
    ('meera_kapoor', 'The Fighting Temeraire',
     'A ghostly old warship is towed to the breaker''s yard by a small steam tug, beneath a blazing sunset.'
     || ' Image: J. M. W. Turner, 1839, public domain (The National Gallery, London). Demo listing.',
     19000.00, 'Painting', 'Oil on canvas', '90.7 x 121.6 cm', 1839, '/images/samples/the-fighting-temeraire.jpg', now() - interval '3 days'),

    ('arjun_rao', 'The Starry Night',
     'Swirling skies over a sleeping village, painted from memory at the asylum in Saint-Rémy: Van Gogh''s most famous night scene.'
     || ' Image: Vincent van Gogh, 1889, public domain (The Museum of Modern Art, New York). Demo listing.',
     24000.00, 'Painting', 'Oil on canvas', '73.7 x 92.1 cm', 1889, '/images/samples/the-starry-night.jpg', now() - interval '2 days'),
    ('arjun_rao', 'Sunflowers',
     'Fourteen sunflowers in every stage of life, painted in Arles to decorate the room awaiting Gauguin''s arrival.'
     || ' Image: Vincent van Gogh, 1888, public domain (The National Gallery, London). Demo listing.',
     18000.00, 'Painting', 'Oil on canvas', '92.1 x 73 cm', 1888, '/images/samples/sunflowers.jpg', now() - interval '4 days'),
    ('arjun_rao', 'The Card Players',
     'Provençal farmhands absorbed in a game of cards: a quiet, monumental scene built from Cézanne''s solid blocks of colour.'
     || ' Image: Paul Cézanne, 1890-92, CC0 (The Metropolitan Museum of Art). Demo listing.',
     14500.00, 'Painting', 'Oil on canvas', NULL, 1890, '/images/samples/the-card-players.jpg', now() - interval '7 days'),

    ('lena_fischer', 'Fine Wind, Clear Morning (Red Fuji)',
     'Mount Fuji glowing red at dawn under a sky of mackerel clouds, one of the great prints from Thirty-six Views of Mount Fuji.'
     || ' Image: Katsushika Hokusai, c. 1830-32, public domain. Demo listing.',
     6800.00, 'Printmaking', 'Woodblock print; ink and colour on paper', NULL, 1831, '/images/samples/red-fuji.jpg', now() - interval '1 day'),
    ('lena_fischer', 'Girl with a Pearl Earring',
     'A girl in a blue and yellow turban turns to look over her shoulder, lips parted, a single pearl catching the light.'
     || ' Image: Johannes Vermeer, c. 1665, public domain (Mauritshuis, The Hague). Demo listing.',
     12000.00, 'Painting', 'Oil on canvas', '44.5 x 39 cm', 1665, '/images/samples/girl-with-a-pearl-earring.jpg', now() - interval '8 days'),

    ('kofi_mensah', 'The Kiss',
     'Two lovers wrapped in a shimmering golden cloak of pattern and ornament: the masterpiece of Klimt''s Golden Period.'
     || ' Image: Gustav Klimt, 1907-08, public domain (Belvedere, Vienna). Demo listing.',
     26000.00, 'Painting', 'Oil and gold leaf on canvas', '180 x 180 cm', 1908, '/images/samples/the-kiss.jpg', now() - interval '2 days'),
    ('kofi_mensah', 'The Dancing Class',
     'Young ballerinas rehearse while their teacher watches: an early example of Degas''s lifelong fascination with dance.'
     || ' Image: Edgar Degas, c. 1870, CC0 (The Metropolitan Museum of Art). Demo listing.',
     11500.00, 'Painting', 'Oil on wood', NULL, 1870, '/images/samples/the-dancing-class.jpg', now() - interval '9 days')
) AS v(username, title, description, price, category, medium, dimensions, creation_year, image_url, created_at)
JOIN app_users u ON u.username = v.username;

INSERT INTO artwork_tags (artwork_id, tag)
SELECT a.id, v.tag
FROM (VALUES
    ('Impression, Sunrise', 'impressionism'), ('Impression, Sunrise', 'water'), ('Impression, Sunrise', 'sunrise'),
    ('Dance at Le Moulin de la Galette', 'impressionism'), ('Dance at Le Moulin de la Galette', 'people'),
    ('The Fighting Temeraire', 'romanticism'), ('The Fighting Temeraire', 'ships'), ('The Fighting Temeraire', 'sunset'),
    ('The Starry Night', 'post-impressionism'), ('The Starry Night', 'night sky'), ('The Starry Night', 'landscape'),
    ('Sunflowers', 'post-impressionism'), ('Sunflowers', 'flowers'), ('Sunflowers', 'still life'),
    ('The Card Players', 'post-impressionism'), ('The Card Players', 'people'),
    ('Fine Wind, Clear Morning (Red Fuji)', 'japan'), ('Fine Wind, Clear Morning (Red Fuji)', 'woodblock'), ('Fine Wind, Clear Morning (Red Fuji)', 'landscape'),
    ('Girl with a Pearl Earring', 'portrait'), ('Girl with a Pearl Earring', 'dutch golden age'),
    ('The Kiss', 'art nouveau'), ('The Kiss', 'gold'), ('The Kiss', 'portrait'),
    ('The Dancing Class', 'impressionism'), ('The Dancing Class', 'ballet')
) AS v(title, tag)
JOIN artworks a ON a.title = v.title AND a.image_url LIKE '/images/samples/%';
