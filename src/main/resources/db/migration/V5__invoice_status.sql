-- Invoices gain a payment status so an invoice can be marked paid. Additive column with a NOT NULL
-- default of 'UNPAID' so every existing row (and any insert that omits it) starts unpaid; the app
-- flips it to 'PAID' when the invoice is paid.
ALTER TABLE invoices ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'UNPAID';
