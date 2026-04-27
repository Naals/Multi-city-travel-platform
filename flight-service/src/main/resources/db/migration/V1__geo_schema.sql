
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";

CREATE TABLE countries (
                           id           UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
                           name         VARCHAR(100) NOT NULL,
                           iso_code     CHAR(2)      NOT NULL,
                           continent    VARCHAR(50)  NOT NULL,
                           currency     CHAR(3),
                           is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
                           created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_countries_iso   ON countries (iso_code);
CREATE UNIQUE INDEX uq_countries_name  ON countries (LOWER(name));
CREATE INDEX idx_countries_continent   ON countries (continent);


CREATE TABLE cities (
                        id            UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
                        country_id    UUID         NOT NULL REFERENCES countries(id),
                        name          VARCHAR(100) NOT NULL,
                        iata_city_code VARCHAR(3),
                        timezone      VARCHAR(60)  NOT NULL,
                        coordinates   GEOGRAPHY(POINT, 4326),
                        population    BIGINT,
                        is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
                        created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cities_country    ON cities (country_id);
CREATE INDEX idx_cities_iata       ON cities (iata_city_code);
CREATE INDEX idx_cities_active     ON cities (is_active) WHERE is_active = TRUE;
CREATE INDEX idx_cities_coordinates ON cities USING GIST (coordinates);

CREATE TABLE airports (
                          id              UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
                          city_id         UUID         NOT NULL REFERENCES cities(id),
                          name            VARCHAR(200) NOT NULL,
                          iata_code       CHAR(3)      NOT NULL,
                          icao_code       CHAR(4),
                          terminal_count  INT          NOT NULL DEFAULT 1,
                          coordinates     GEOGRAPHY(POINT, 4326),
                          is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
                          created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_airports_iata  ON airports (iata_code);
CREATE INDEX idx_airports_city        ON airports (city_id);
CREATE INDEX idx_airports_active      ON airports (is_active) WHERE is_active = TRUE;