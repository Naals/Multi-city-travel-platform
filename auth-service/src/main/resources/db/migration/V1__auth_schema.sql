CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Roles available across the platform
CREATE TYPE user_role AS ENUM ('ROLE_USER', 'ROLE_ADMIN', 'ROLE_MODERATOR');

CREATE TABLE user_credentials (
                                  id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                  email         VARCHAR(255) NOT NULL,
                                  password_hash VARCHAR(255) NOT NULL,
                                  role          user_role    NOT NULL DEFAULT 'ROLE_USER',
                                  is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
                                  is_locked     BOOLEAN      NOT NULL DEFAULT FALSE,
                                  failed_login_attempts  INT NOT NULL DEFAULT 0,
                                  last_login_at TIMESTAMPTZ,
                                  created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                                  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_user_credentials_email
    ON user_credentials (LOWER(email));

CREATE INDEX idx_user_credentials_role
    ON user_credentials (role);

CREATE INDEX idx_user_credentials_active
    ON user_credentials (is_active) WHERE is_active = TRUE;


CREATE OR REPLACE FUNCTION fn_update_updated_at()
    RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_user_credentials_updated_at
    BEFORE UPDATE ON user_credentials
    FOR EACH ROW EXECUTE FUNCTION fn_update_updated_at();

CREATE TABLE auth_audit_log (
                                id          BIGSERIAL    PRIMARY KEY,
                                user_id     UUID         REFERENCES user_credentials(id) ON DELETE SET NULL,
                                email       VARCHAR(255),
                                event_type  VARCHAR(50)  NOT NULL, -- LOGIN_SUCCESS, LOGIN_FAIL, LOGOUT, TOKEN_REFRESH, ACCOUNT_LOCKED
                                ip_address  INET,
                                user_agent  TEXT,
                                occurred_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auth_audit_user    ON auth_audit_log (user_id);
CREATE INDEX idx_auth_audit_event   ON auth_audit_log (event_type);
CREATE INDEX idx_auth_audit_time    ON auth_audit_log (occurred_at DESC);