-- Invoices gain two dates: when the invoice was issued (went out) and when it is due. Additive
-- columns stored as ISO date strings (YYYY-MM-DD) so the value the app surfaces is exactly what was
-- stored — the front-end shows the literal date. Nullable so existing rows are unaffected; the
-- /__test__ seed supplies both for a fixture invoice.
ALTER TABLE invoices ADD COLUMN issued_date VARCHAR(10);
ALTER TABLE invoices ADD COLUMN due_date    VARCHAR(10);
