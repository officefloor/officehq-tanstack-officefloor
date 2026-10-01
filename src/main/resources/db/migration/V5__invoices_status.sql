-- Invoices carry a payment status so a project's detail page can show UNPAID/PAID and mark one paid.
-- New invoices default to UNPAID; existing rows adopt the same default via the column default.
ALTER TABLE invoices ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'UNPAID';
