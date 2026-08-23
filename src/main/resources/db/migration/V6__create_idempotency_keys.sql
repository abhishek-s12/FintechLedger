-- V6: Create Idempotency Keys Table
CREATE TABLE IF NOT EXISTS idempotency_keys (
    id UUID PRIMARY KEY,
    key_value VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    request_hash VARCHAR(255) NOT NULL,
    response_code INT,
    response_body TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PROCESSING', -- 'PROCESSING', 'COMPLETED', 'FAILED'
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_idempotency_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_idempotency_key ON idempotency_keys(key_value);
CREATE INDEX IF NOT EXISTS idx_idempotency_user ON idempotency_keys(user_id);
