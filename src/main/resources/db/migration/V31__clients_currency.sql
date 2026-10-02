-- Each client is billed in their own currency. A new column holding the client's currency code
-- (e.g. USD, EUR), defaulting to USD so every existing client keeps being shown in dollars until it
-- is changed. Money the client is shown anywhere is formatted in this currency.
ALTER TABLE clients ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'USD';
