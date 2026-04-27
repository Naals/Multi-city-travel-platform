CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE review_eligibility (
                                    id           UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
                                    user_id      UUID        NOT NULL,
                                    flight_id    UUID        NOT NULL,
                                    booking_id   UUID        NOT NULL,
                                    trip_id      UUID        NOT NULL,
                                    eligible_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                    reviewed     BOOLEAN     NOT NULL DEFAULT FALSE,
                                    CONSTRAINT uq_eligibility UNIQUE (user_id, flight_id, booking_id)
);

CREATE INDEX idx_eligibility_user    ON review_eligibility (user_id);
CREATE INDEX idx_eligibility_flight  ON review_eligibility (flight_id);
CREATE INDEX idx_eligibility_pending
    ON review_eligibility (user_id) WHERE reviewed = FALSE;

CREATE TABLE reviews (
                         id           UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
                         eligibility_id UUID       NOT NULL REFERENCES review_eligibility(id),
                         user_id      UUID         NOT NULL,
                         flight_id    UUID         NOT NULL,
                         booking_id   UUID         NOT NULL,
                         overall_rating      SMALLINT NOT NULL CHECK (overall_rating BETWEEN 1 AND 5),
                         punctuality_rating  SMALLINT CHECK (punctuality_rating BETWEEN 1 AND 5),
                         comfort_rating      SMALLINT CHECK (comfort_rating BETWEEN 1 AND 5),
                         service_rating      SMALLINT CHECK (service_rating BETWEEN 1 AND 5),
                         value_rating        SMALLINT CHECK (value_rating BETWEEN 1 AND 5),
                         title        VARCHAR(200),
                         comment      TEXT,
                         is_published BOOLEAN      NOT NULL DEFAULT TRUE,
                         is_flagged   BOOLEAN      NOT NULL DEFAULT FALSE,
                         flag_reason  VARCHAR(200),
                         helpful_votes INT         NOT NULL DEFAULT 0,
                         created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                         updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_review_per_booking
    ON reviews (booking_id);

CREATE INDEX idx_reviews_flight     ON reviews (flight_id);
CREATE INDEX idx_reviews_user       ON reviews (user_id);
CREATE INDEX idx_reviews_rating     ON reviews (overall_rating);
CREATE INDEX idx_reviews_published  ON reviews (is_published, created_at DESC)
    WHERE is_published = TRUE;

CREATE OR REPLACE FUNCTION fn_update_updated_at()
    RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END; $$;

CREATE TRIGGER trg_reviews_updated_at
    BEFORE UPDATE ON reviews
    FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE MATERIALIZED VIEW mv_flight_ratings AS
SELECT
    flight_id,
    COUNT(*)                              AS review_count,
    ROUND(AVG(overall_rating), 2)         AS avg_overall,
    ROUND(AVG(punctuality_rating), 2)     AS avg_punctuality,
    ROUND(AVG(comfort_rating), 2)         AS avg_comfort,
    ROUND(AVG(service_rating), 2)         AS avg_service,
    ROUND(AVG(value_rating), 2)           AS avg_value
FROM reviews
WHERE is_published = TRUE
GROUP BY flight_id;

CREATE UNIQUE INDEX idx_mv_flight_ratings ON mv_flight_ratings (flight_id);