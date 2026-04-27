CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE gender_type AS ENUM ('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY');

CREATE TABLE users (
                       id              UUID         PRIMARY KEY,
                       first_name      VARCHAR(100) NOT NULL,
                       last_name       VARCHAR(100) NOT NULL,
                       email           VARCHAR(255) NOT NULL,
                       phone           VARCHAR(30),
                       date_of_birth   DATE,
                       gender          gender_type,
                       nationality     VARCHAR(100),
                       passport_number VARCHAR(50),
                       passport_expiry DATE,
                       preferences     JSONB        NOT NULL DEFAULT '{}',
                       avatar_url      VARCHAR(500),
                       is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                       updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_users_email
    ON users (LOWER(email));

CREATE INDEX idx_users_name
    ON users (last_name, first_name);

CREATE INDEX idx_users_nationality
    ON users (nationality);

CREATE INDEX idx_users_preferences
    ON users USING GIN (preferences);


CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();


CREATE TABLE saved_routes (
                              id               UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
                              user_id          UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                              origin_city_code VARCHAR(10) NOT NULL,
                              dest_city_code   VARCHAR(10) NOT NULL,
                              label            VARCHAR(100),
                              created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_saved_routes_user ON saved_routes (user_id);
CREATE UNIQUE INDEX uq_saved_routes_user_path
    ON saved_routes (user_id, origin_city_code, dest_city_code);