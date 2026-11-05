-- 001_initial_schema.sql (Hearth PostgreSQL Schema - all money in BIGINT paisa)

CREATE TABLE IF NOT EXISTS household (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    invite_code VARCHAR(30) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS settings (
    key VARCHAR(50) PRIMARY KEY,
    value VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS members (
    id VARCHAR(10) PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    email VARCHAR(120) UNIQUE NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('admin', 'member')),
    failed_attempts INT DEFAULT 0,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id SERIAL PRIMARY KEY,
    member_id VARCHAR(10) REFERENCES members(id) ON DELETE CASCADE,
    family_id VARCHAR(50) NOT NULL,
    token_hash VARCHAR(64) UNIQUE NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by VARCHAR(64),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS shopping_items (
    id VARCHAR(20) PRIMARY KEY,
    title VARCHAR(80) NOT NULL,
    unit VARCHAR(20) NOT NULL CHECK (unit IN ('pack', 'dozen', 'bottle', 'bag')),
    requested_qty INT NOT NULL CHECK (requested_qty > 0 AND requested_qty <= 999),
    status VARCHAR(20) NOT NULL CHECK (status IN ('pending', 'bought')),
    expense_id VARCHAR(20),
    created_by VARCHAR(10) REFERENCES members(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    bought_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS expenses (
    id VARCHAR(20) PRIMARY KEY,
    title VARCHAR(80) NOT NULL,
    category VARCHAR(40) NOT NULL,
    amount_paisa BIGINT NOT NULL CHECK (amount_paisa > 0),
    payer_id VARCHAR(10) NOT NULL REFERENCES members(id),
    split_type VARCHAR(20) NOT NULL CHECK (split_type IN ('EQUAL', 'CUSTOM')),
    expense_date DATE NOT NULL,
    source VARCHAR(20) NOT NULL CHECK (source IN ('manual', 'checkout', 'recurring')),
    created_by VARCHAR(10) NOT NULL REFERENCES members(id),
    subtotal_paisa BIGINT,
    tax_percent NUMERIC(5,2),
    discount_paisa BIGINT,
    is_recurring BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS expense_splits (
    expense_id VARCHAR(20) REFERENCES expenses(id) ON DELETE CASCADE,
    member_id VARCHAR(10) REFERENCES members(id),
    share_paisa BIGINT NOT NULL CHECK (share_paisa >= 0),
    PRIMARY KEY (expense_id, member_id)
);

CREATE TABLE IF NOT EXISTS settlements (
    id VARCHAR(20) PRIMARY KEY,
    sender_id VARCHAR(10) NOT NULL REFERENCES members(id),
    recipient_id VARCHAR(10) NOT NULL REFERENCES members(id),
    amount_paisa BIGINT NOT NULL CHECK (amount_paisa > 0),
    recorded_by VARCHAR(10) NOT NULL REFERENCES members(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CHECK (sender_id <> recipient_id)
);

CREATE TABLE IF NOT EXISTS recurring_templates (
    id VARCHAR(20) PRIMARY KEY,
    title VARCHAR(80) NOT NULL,
    category VARCHAR(40) NOT NULL,
    amount_paisa BIGINT NOT NULL,
    payer_id VARCHAR(10) REFERENCES members(id),
    split_type VARCHAR(20) NOT NULL,
    splits_json JSONB NOT NULL,
    active BOOLEAN DEFAULT true
);

CREATE TABLE IF NOT EXISTS recurring_runs (
    template_id VARCHAR(20) REFERENCES recurring_templates(id),
    month CHAR(7) NOT NULL,
    expense_id VARCHAR(20) REFERENCES expenses(id),
    UNIQUE (template_id, month)
);

CREATE TABLE IF NOT EXISTS idempotency_keys (
    key VARCHAR(100) NOT NULL,
    endpoint VARCHAR(100) NOT NULL,
    member_id VARCHAR(10) NOT NULL,
    status INT NOT NULL,
    response JSONB NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (key, endpoint)
);

CREATE INDEX IF NOT EXISTS idx_expenses_date ON expenses(expense_date);
CREATE INDEX IF NOT EXISTS idx_expenses_payer ON expenses(payer_id);
CREATE INDEX IF NOT EXISTS idx_shopping_status ON shopping_items(status);
CREATE INDEX IF NOT EXISTS idx_refresh_token_hash ON refresh_tokens(token_hash);
