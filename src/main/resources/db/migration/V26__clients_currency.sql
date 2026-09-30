-- Each client is paid in their own currency. A client's money is shown in this currency everywhere
-- it is surfaced (the invoice rows, the statement, the dashboard totals). Existing clients default to
-- USD; the user can change a client's currency from their detail page.
ALTER TABLE clients ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'USD';
