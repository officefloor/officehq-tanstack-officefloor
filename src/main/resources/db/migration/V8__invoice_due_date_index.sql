-- Sorting a project's invoices by due date is now a first-class read path (the detail page offers a
-- "sort by due date" control). Index the due_date column so the earliest-due-first ordering is served
-- from an index rather than a full scan as a project accumulates invoices. Additive and idempotent.
CREATE INDEX IF NOT EXISTS idx_invoices_due_date ON invoices (due_date);
