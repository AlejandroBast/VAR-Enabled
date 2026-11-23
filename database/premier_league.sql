
CREATE TABLE IF NOT EXISTS ligas (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    pais VARCHAR(100) NOT NULL,
    temporada VARCHAR(20),
    logo VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS equipos (
    id SERIAL PRIMARY KEY,
    liga_id INTEGER NOT NULL REFERENCES ligas(id),
    nombre VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    logo VARCHAR(255) NOT NULL
);

INSERT INTO ligas (nombre, pais, temporada, logo)
VALUES (
    'Premier League',
    'Inglaterra',
    '2026/27',
    '/assets/ligas/premier-league.svg'
)
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO equipos (liga_id, nombre, slug, logo)
SELECT l.id, e.nombre, e.slug,
       '/assets/equipos/' || e.slug || '.svg'
FROM ligas l
CROSS JOIN (
    VALUES
    ('Arsenal', 'arsenal'),
    ('Aston Villa', 'aston-villa'),
    ('Bournemouth', 'bournemouth'),
    ('Brentford', 'brentford'),
    ('Brighton', 'brighton'),
    ('Chelsea', 'chelsea'),
    ('Coventry City', 'coventry-city'),
    ('Crystal Palace', 'crystal-palace'),
    ('Everton', 'everton'),
    ('Fulham', 'fulham'),
    ('Hull City', 'hull-city'),
    ('Ipswich Town', 'ipswich-town'),
    ('Leeds United', 'leeds-united'),
    ('Liverpool', 'liverpool'),
    ('Manchester City', 'manchester-city'),
    ('Manchester United', 'manchester-united'),
    ('Newcastle United', 'newcastle-united'),
    ('Nottingham Forest', 'nottingham-forest'),
    ('Sunderland', 'sunderland'),
    ('Tottenham Hotspur', 'tottenham')
) AS e(nombre, slug)
WHERE l.nombre = 'Premier League'
ON CONFLICT (slug) DO NOTHING;

SELECT COUNT(*) AS total_equipos
FROM equipos;
