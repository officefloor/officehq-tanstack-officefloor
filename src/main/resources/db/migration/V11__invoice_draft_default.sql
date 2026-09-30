-- Invoices now move through a lifecycle: DRAFT -> SENT -> PAID. A freshly created invoice starts as
-- a DRAFT (it has not been sent yet), so change the column default to match the create path
-- (InvoicesPost). Additive: only the default for new inserts changes; existing rows keep their
-- stored status.
ALTER TABLE invoice ALTER COLUMN status SET DEFAULT 'DRAFT';
