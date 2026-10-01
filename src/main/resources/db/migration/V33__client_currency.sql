-- Each client is paid in their own currency, so their money is shown in it everywhere. Add the
-- currency the client bills in: a short ISO code, defaulting to USD for every existing client (the
-- currency the app showed before this change), and constrained to the set the app can render a
-- symbol for. A new client starts USD until the user sets it.
ALTER TABLE clients ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'USD';
ALTER TABLE clients ADD CONSTRAINT clients_currency_allowed CHECK (currency IN ('USD', 'EUR', 'GBP'));
