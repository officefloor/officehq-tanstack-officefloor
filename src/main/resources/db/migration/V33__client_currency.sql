-- Each client is billed in their own currency (USD or EUR). Their money is shown in that currency
-- everywhere, and the home-screen totals are kept separate per currency. Existing clients default to
-- USD, the currency the app used before this column existed.
ALTER TABLE client ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'USD';
