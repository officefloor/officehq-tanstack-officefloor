-- Narrowing the all-invoices list to a single stage is now a first-class read path (the invoices
-- page offers a status filter). Index the status column so filtering to one stage (DRAFT, SENT,
-- PAID) is served from an index rather than a full scan as invoices accumulate. Additive and
-- idempotent.
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices (status);
