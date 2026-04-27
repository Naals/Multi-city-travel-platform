CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE booking_status AS ENUM (
    'PENDING',
    'CONFIRMED',
    'CANCELLED',
    'COMPLETED',
    'REFUND_PENDING',
    'REFUNDED'
    );

CREATE TYPE cancellation_reason AS ENUM (
    'USER_REQUESTED',
    'PAYMENT_FAILED',
    'FLIGHT_CANCELLED',
    'FLIGHT_DELAYED_UNACCEPTABLE',
    'OVERBOOKING',
    'SYSTEM_ERROR'
    );

CREATE TABLE bookings (
                          id                  UUID              PRIMARY KEY DEFAULT uuid_generate_v4(),
                          trip_id             UUID              NOT NULL DEFAULT uuid_generate_v4(),
                          user_id             UUID              NOT NULL,
                          flight_id           UUID              NOT NULL,
                          flight_number       VARCHAR(10)       NOT NULL,
                          origin_iata         CHAR(3)           NOT NULL,
                          dest_iata           CHAR(3)           NOT NULL,
                          departure_at        TIMESTAMPTZ       NOT NULL,
                          arrival_at          TIMESTAMPTZ       NOT NULL,
                          cabin               VARCHAR(20)       NOT NULL,
                          price_paid          NUMERIC(10,2)     NOT NULL,
                          currency            CHAR(3)           NOT NULL DEFAULT 'USD',
                          seat_number         VARCHAR(5),
                          passengers          JSONB             NOT NULL DEFAULT '[]',
                          status              booking_status    NOT NULL DEFAULT 'PENDING',
                          cancellation_reason cancellation_reason,
                          cancelled_at        TIMESTAMPTZ,
                          cancel_note         TEXT,
                          booked_at           TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
                          confirmed_at        TIMESTAMPTZ,
                          completed_at        TIMESTAMPTZ,
                          updated_at          TIMESTAMPTZ       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bookings_user         ON bookings (user_id);
CREATE INDEX idx_bookings_flight       ON bookings (flight_id);
CREATE INDEX idx_bookings_trip         ON bookings (trip_id);
CREATE INDEX idx_bookings_status       ON bookings (status);
CREATE INDEX idx_bookings_booked_at    ON bookings (booked_at DESC);
CREATE INDEX idx_bookings_active
    ON bookings (user_id, status)
    WHERE status IN ('PENDING', 'CONFIRMED');

CREATE OR REPLACE FUNCTION fn_update_updated_at()
    RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END; $$;

CREATE TRIGGER trg_bookings_updated_at
    BEFORE UPDATE ON bookings
    FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TABLE booking_events (
                                id           BIGSERIAL      PRIMARY KEY,
                                booking_id   UUID           NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
                                from_status  booking_status,
                                to_status    booking_status NOT NULL,
                                triggered_by VARCHAR(50)    NOT NULL,
                                note         TEXT,
                                occurred_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_booking_events_booking ON booking_events (booking_id);
CREATE INDEX idx_booking_events_time    ON booking_events (occurred_at DESC);