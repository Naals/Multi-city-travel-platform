CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE payment_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED',
    'REFUND_PENDING',
    'REFUNDED',
    'PARTIALLY_REFUNDED',
    'DISPUTED'
    );

CREATE TYPE payment_method AS ENUM (
    'CREDIT_CARD',
    'DEBIT_CARD',
    'PAYPAL',
    'BANK_TRANSFER',
    'CRYPTO',
    'WALLET'
    );

CREATE TABLE payments (
                          id                UUID           PRIMARY KEY DEFAULT uuid_generate_v4(),
                          booking_id        UUID           NOT NULL,
                          user_id           UUID           NOT NULL,
                          idempotency_key   VARCHAR(100)   NOT NULL,
                          amount            NUMERIC(10,2)  NOT NULL CHECK (amount > 0),
                          currency          CHAR(3)        NOT NULL DEFAULT 'USD',
                          method            payment_method NOT NULL,
                          status            payment_status NOT NULL DEFAULT 'PENDING',
                          gateway_name      VARCHAR(50),
                          gateway_tx_id     VARCHAR(200),
                          gateway_response  JSONB,
                          card_last_four    CHAR(4),
                          card_brand        VARCHAR(20),
                          refunded_amount   NUMERIC(10,2)  NOT NULL DEFAULT 0,
                          refund_reason     TEXT,
                          refunded_at       TIMESTAMPTZ,
                          initiated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
                          completed_at      TIMESTAMPTZ,
                          updated_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_payments_idempotency
    ON payments (idempotency_key);

CREATE INDEX idx_payments_booking    ON payments (booking_id);
CREATE INDEX idx_payments_user       ON payments (user_id);
CREATE INDEX idx_payments_status     ON payments (status);
CREATE INDEX idx_payments_gateway_tx ON payments (gateway_tx_id)
    WHERE gateway_tx_id IS NOT NULL;

CREATE OR REPLACE FUNCTION fn_update_updated_at()
    RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END; $$;

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();


CREATE TABLE payment_audit (
                               id             BIGSERIAL      PRIMARY KEY,
                               payment_id     UUID           NOT NULL REFERENCES payments(id),
                               from_status    payment_status,
                               to_status      payment_status NOT NULL,
                               amount         NUMERIC(10,2),
                               note           TEXT,
                               metadata       JSONB,
                               occurred_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_audit_payment ON payment_audit (payment_id);
CREATE INDEX idx_payment_audit_time    ON payment_audit (occurred_at DESC);