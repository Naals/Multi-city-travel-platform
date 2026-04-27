-- ============================================================
-- FLIGHTS & DIJKSTRA GRAPH SCHEMA
-- flights       = actual scheduled flight instances
-- flight_routes = weighted directed graph edges for Dijkstra
--                 origin_city → dest_city with price/duration weight
-- ============================================================

CREATE TYPE flight_status AS ENUM (
    'SCHEDULED',    -- normal, bookable
    'DELAYED',      -- still operating, new departure time set
    'CANCELLED',    -- fully cancelled, triggers cascade
    'BOARDING',     -- gate open, no new bookings
    'DEPARTED',     -- in air
    'ARRIVED',      -- landed
    'DIVERTED'      -- landed at different airport
    );

CREATE TYPE cabin_class AS ENUM (
    'ECONOMY', 'PREMIUM_ECONOMY', 'BUSINESS', 'FIRST'
    );

CREATE TABLE flights (
                         id                  UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
                         flight_number       VARCHAR(10)   NOT NULL,       -- TK001, LH404
                         airline_code        CHAR(2)       NOT NULL,       -- TK, LH, AA
                         airline_name        VARCHAR(100)  NOT NULL,
                         origin_airport_id   UUID          NOT NULL REFERENCES airports(id),
                         dest_airport_id     UUID          NOT NULL REFERENCES airports(id),
    -- Scheduled times
                         scheduled_departure TIMESTAMPTZ   NOT NULL,
                         scheduled_arrival   TIMESTAMPTZ   NOT NULL,
    -- Actual times (updated during operation)
                         actual_departure    TIMESTAMPTZ,
                         actual_arrival      TIMESTAMPTZ,
    -- Delay tracking
                         delay_minutes       INT           NOT NULL DEFAULT 0,
                         delay_reason        VARCHAR(255),
    -- Capacity
                         seats_total         INT           NOT NULL CHECK (seats_total > 0),
                         seats_booked        INT           NOT NULL DEFAULT 0 CHECK (seats_booked >= 0),
                         seats_available     INT GENERATED ALWAYS AS (seats_total - seats_booked) STORED,
                         cabin               cabin_class   NOT NULL DEFAULT 'ECONOMY',
    -- Pricing (base price, can surge)
                         base_price          NUMERIC(10,2) NOT NULL CHECK (base_price >= 0),
                         current_price       NUMERIC(10,2) NOT NULL CHECK (current_price >= 0),
                         currency            CHAR(3)       NOT NULL DEFAULT 'USD',
    -- Duration in minutes (computed but stored for graph efficiency)
                         duration_minutes    INT GENERATED ALWAYS AS (
                             EXTRACT(EPOCH FROM (scheduled_arrival - scheduled_departure)) / 60
                             )::INT STORED,
    -- State
                         status              flight_status NOT NULL DEFAULT 'SCHEDULED',
                         aircraft_type       VARCHAR(20),   -- B737, A320, B777
                         is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
                         created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                         updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- Core query indexes
CREATE UNIQUE INDEX uq_flight_number_departure
    ON flights (flight_number, scheduled_departure);
CREATE INDEX idx_flights_origin       ON flights (origin_airport_id);
CREATE INDEX idx_flights_dest         ON flights (dest_airport_id);
CREATE INDEX idx_flights_departure    ON flights (scheduled_departure);
CREATE INDEX idx_flights_status       ON flights (status);
CREATE INDEX idx_flights_airline      ON flights (airline_code);
-- Partial index: only active, scheduleable flights
CREATE INDEX idx_flights_bookable
    ON flights (scheduled_departure, base_price)
    WHERE status = 'SCHEDULED' AND seats_available > 0;

-- Auto updated_at
CREATE OR REPLACE FUNCTION fn_update_updated_at()
    RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END; $$;

CREATE TRIGGER trg_flights_updated_at
    BEFORE UPDATE ON flights
    FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

-- ============================================================
-- DIJKSTRA GRAPH TABLE
-- Materialized edges between cities (not airports).
-- Each active flight between two cities = one directed edge.
-- Multiple flights on same route = multiple edges (cheapest wins).
-- This table is rebuilt by GraphBuilderService on startup + cache miss.
-- ============================================================
CREATE TABLE flight_routes (
                               id                  UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
                               flight_id           UUID          NOT NULL REFERENCES flights(id) ON DELETE CASCADE,
                               origin_city_id      UUID          NOT NULL REFERENCES cities(id),
                               dest_city_id        UUID          NOT NULL REFERENCES cities(id),
    -- Two weight dimensions — Dijkstra uses one at a time based on sortBy param
                               weight_price        NUMERIC(10,2) NOT NULL,
                               weight_duration_min INT           NOT NULL,
    -- Layover feasibility — minimum connection time at dest airport
                               min_connection_min  INT           NOT NULL DEFAULT 60,
    -- Denormalized for fast graph loading
                               departure_at        TIMESTAMPTZ   NOT NULL,
                               arrival_at          TIMESTAMPTZ   NOT NULL,
                               airline_code        CHAR(2)       NOT NULL,
                               cabin               cabin_class   NOT NULL,
                               is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
                               updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_flight_routes_origin      ON flight_routes (origin_city_id);
CREATE INDEX idx_flight_routes_dest        ON flight_routes (dest_city_id);
CREATE INDEX idx_flight_routes_active      ON flight_routes (is_active) WHERE is_active = TRUE;
CREATE INDEX idx_flight_routes_departure   ON flight_routes (departure_at);
-- Composite for Dijkstra edge lookup
CREATE INDEX idx_flight_routes_graph
    ON flight_routes (origin_city_id, dest_city_id, weight_price)
    WHERE is_active = TRUE;

CREATE TABLE seat_inventory (
                                id              UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
                                flight_id       UUID          NOT NULL REFERENCES flights(id) ON DELETE CASCADE,
                                cabin           cabin_class   NOT NULL,
                                seats_total     INT           NOT NULL CHECK (seats_total > 0),
                                seats_booked    INT           NOT NULL DEFAULT 0,
                                seats_available INT GENERATED ALWAYS AS (seats_total - seats_booked) STORED,
                                price           NUMERIC(10,2) NOT NULL,
                                updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                                CONSTRAINT uq_seat_inventory UNIQUE (flight_id, cabin)
);

CREATE INDEX idx_seat_inventory_flight ON seat_inventory (flight_id);