-- Invoices gain a payment status so one can be marked paid. New invoices start UNPAID; paying an
-- invoice flips it to PAID (see InvoicePay). Additive column with a default, so existing rows and the
-- create path need no change.
ALTER TABLE invoice ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'UNPAID';
