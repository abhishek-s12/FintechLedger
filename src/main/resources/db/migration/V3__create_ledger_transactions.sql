-- V3: Create Ledger Transactions Table
CREATE TABLE IF NOT EXISTS ledger_transactions (
    id UUID PRIMARY KEY,
    reference_id VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ledger_tx_ref ON ledger_transactions(reference_id);
CREATE INDEX IF NOT EXISTS idx_ledger_tx_type ON ledger_transactions(type);
CREATE INDEX IF NOT EXISTS idx_ledger_tx_created ON ledger_transactions(created_at);
