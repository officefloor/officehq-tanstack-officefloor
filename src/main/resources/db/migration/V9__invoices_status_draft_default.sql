-- Invoices now move through a lifecycle: DRAFT -> SENT -> PAID. A freshly raised invoice starts as a
-- DRAFT (it is sent later, which is what records an audit entry and unlocks payment), so the column
-- default becomes DRAFT. Existing rows keep whatever status they already hold.
ALTER TABLE invoices ALTER COLUMN status SET DEFAULT 'DRAFT';
