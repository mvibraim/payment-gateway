-- Tables are defined with CREATE TABLE IF NOT EXISTS (ADR-0002).
-- No full card number or CVV is ever stored in payments (BR-005, BR-006).
CREATE TABLE IF NOT EXISTS payments (
    id TEXT PRIMARY KEY,
    merchant_id TEXT NOT NULL,
    last4 TEXT NOT NULL,
    expiry_month INTEGER NOT NULL,
    expiry_year INTEGER NOT NULL,
    amount INTEGER NOT NULL,
    currency TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at TEXT NOT NULL
);

-- The simulated bank's own record of requests and charges (ADR-0004). Gateway code never reads it.
CREATE TABLE IF NOT EXISTS bank_requests (
    payment_id TEXT PRIMARY KEY,
    amount INTEGER NOT NULL,
    currency TEXT NOT NULL,
    result TEXT NOT NULL,
    charged INTEGER NOT NULL
);
